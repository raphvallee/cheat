package dev.cheat.gradle.patch;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One hook call injected into a vanilla method. Built with a small fluent DSL:
 *
 * <pre>{@code
 * Injection.into("net/minecraft/client/Minecraft")
 *         .method("tick", "()V")
 *         .at(At.HEAD)
 *         .call("onTickPre");
 * }</pre>
 *
 * <p>The hook is a {@code public static} method on the client's {@code Hooks} class. Its parameters are
 * always the target instance ({@code this}, omitted for static targets) followed by every parameter of the
 * target method, in order. It returns {@code void}, or {@code boolean} when {@link #cancellable()} is used:
 * returning {@code true} makes the vanilla method return immediately.</p>
 */
public final class Injection {
    final String owner;
    String name;
    String desc;
    At at;
    String hook;
    boolean cancellable;
    String invokeOwner;
    String invokeName;
    String invokeDesc;
    int ordinal = 0;
    private Injection(String owner) {
        this.owner = owner;
    }

    public static Injection into(String owner) {
        return new Injection(owner);
    }

    public Injection method(String name, String desc) {
        this.name = name;
        this.desc = desc;
        return this;
    }

    public Injection at(At at) {
        this.at = at;
        return this;
    }

    /**
     * Injects right before the {@code ordinal}-th call (0-based) to the given method inside the target.
     */
    public Injection beforeInvoke(String owner, String name, String desc, int ordinal) {
        this.at = At.INVOKE;
        this.invokeOwner = owner;
        this.invokeName = name;
        this.invokeDesc = desc;
        this.ordinal = ordinal;
        return this;
    }

    public Injection beforeInvoke(String owner, String name, String desc) {
        return beforeInvoke(owner, name, desc, 0);
    }

    public Injection cancellable() {
        this.cancellable = true;
        return this;
    }

    public Injection call(String hook) {
        this.hook = hook;
        return this;
    }

    String target() {
        return owner + "." + name + desc;
    }

    /**
     * Descriptor the hook method must have.
     */
    String hookDesc(boolean targetIsStatic) {
        List<Type> params = new ArrayList<>();
        if (!targetIsStatic) params.add(Type.getObjectType(owner));
        params.addAll(List.of(Type.getArgumentTypes(desc)));
        return Type.getMethodDescriptor(cancellable ? Type.BOOLEAN_TYPE : Type.VOID_TYPE, params.toArray(Type[]::new));
    }

    void validate() {
        Objects.requireNonNull(name, "method() not set for injection into " + owner);
        Objects.requireNonNull(at, "at() not set for " + target());
        Objects.requireNonNull(hook, "call() not set for " + target());
        if (cancellable && at == At.RETURN) {
            throw new IllegalStateException(target() + ": RETURN injections cannot be cancellable");
        }
        if (cancellable && Type.getReturnType(desc) != Type.VOID_TYPE) {
            throw new IllegalStateException(target() + ": cancellable injections only support void methods");
        }
        if (at == At.RETURN && Type.getReturnType(desc) != Type.VOID_TYPE) {
            throw new IllegalStateException(target() + ": RETURN injections only support void methods");
        }
        if (name.equals("<init>") && at != At.RETURN) {
            throw new IllegalStateException(target() + ": constructors only support RETURN injections");
        }
    }

    String unsupportedReason(MethodNode method) {
        if (method.instructions.size() == 0) return "the target method has no code (abstract or native)";
        if (cancellable && at == At.INVOKE) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn.getOpcode() == Opcodes.MONITORENTER) {
                    return "cancellable INVOKE injections are not supported in methods with synchronized blocks";
                }
            }
        }
        return null;
    }

    /**
     * Applies this injection to {@code method}. Returns how many call sites were inserted.
     */
    int apply(MethodNode method, String hooksClass) {
        boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
        String hookDesc = hookDesc(isStatic);
        List<AbstractInsnNode> anchors = new ArrayList<>();
        switch (at) {
            case HEAD -> anchors.add(method.instructions.getFirst());
            case RETURN -> {
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn.getOpcode() == Opcodes.RETURN) anchors.add(insn);
                }
            }
            case INVOKE -> {
                int seen = 0;
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof MethodInsnNode m && m.owner.equals(invokeOwner)
                            && m.name.equals(invokeName) && m.desc.equals(invokeDesc)) {
                        if (seen++ == ordinal) {
                            anchors.add(insn);
                            break;
                        }
                    }
                }
            }
        }
        for (AbstractInsnNode anchor : anchors) {
            InsnList call = new InsnList();
            int local = 0;
            if (!isStatic) {
                call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                local = 1;
            }
            for (Type arg : Type.getArgumentTypes(method.desc)) {
                call.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD), local));
                local += arg.getSize();
            }
            call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, hooksClass, hook, hookDesc, false));
            if (cancellable) {
                LabelNode proceed = new LabelNode();
                call.add(new JumpInsnNode(Opcodes.IFEQ, proceed));
                call.add(new InsnNode(Opcodes.RETURN));
                call.add(proceed);
            }
            method.instructions.insertBefore(anchor, call);
        }
        return anchors.size();
    }

    @Override
    public String toString() {
        String where = at == At.INVOKE ? "before " + invokeOwner + "." + invokeName + invokeDesc + " #" + ordinal : at.name();
        return target() + " @ " + where + " -> " + hook + (cancellable ? " (cancellable)" : "");
    }

    public enum At {HEAD, RETURN, INVOKE}
}
