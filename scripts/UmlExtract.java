import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import javax.lang.model.element.Modifier;
import javax.tools.ToolProvider;

/**
 * Prints every top-level and nested type in a source tree with its fields and method signatures
 * in UML notation, one tab-separated line per member, so the class diagram can be checked
 * against (and generated from) the real code. Usage: java scripts/UmlExtract.java src/main/java
 */
public final class UmlExtract {
    public static void main(String[] args) throws Exception {
        List<Path> files;
        try (var walk = Files.walk(Path.of(args[0]))) {
            files = walk.filter(p -> p.toString().endsWith(".java")).sorted().collect(Collectors.toList());
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var fm = compiler.getStandardFileManager(null, null, null)) {
            var task = (JavacTask) compiler.getTask(null, fm, null, List.of("-proc:none"), null, fm.getJavaFileObjectsFromPaths(files));
            for (CompilationUnitTree unit : task.parse()) new Printer().scan(unit, null);
        }
    }

    private static final class Printer extends TreeScanner<Void, Void> {
        private final Deque<String> owners = new ArrayDeque<>();

        @Override public Void visitClass(ClassTree type, Void unused) {
            if (type.getSimpleName().isEmpty()) return null;
            String name = owners.isEmpty() ? type.getSimpleName().toString() : owners.peek() + "." + type.getSimpleName();
            System.out.println("TYPE\t" + name + "\t" + type.getKind() + "\t" + supertypes(type));
            if (type.getKind() == Tree.Kind.RECORD) {
                for (Tree member : type.getMembers()) {
                    if (member instanceof VariableTree v && !v.getModifiers().getFlags().contains(Modifier.STATIC)) {
                        System.out.println("FIELD\t" + name + "\t-" + v.getName() + ": " + v.getType());
                    }
                }
            }
            owners.push(name);
            for (Tree member : type.getMembers()) {
                if (member instanceof VariableTree v) field(name, v, type.getKind());
                else if (member instanceof MethodTree m) method(name, m, type);
                else if (member instanceof ClassTree nested) visitClass(nested, null);
            }
            owners.pop();
            return null;
        }

        private static String supertypes(ClassTree type) {
            var parts = new ArrayList<String>();
            if (type.getExtendsClause() != null) parts.add("extends " + type.getExtendsClause());
            for (Tree t : type.getImplementsClause()) parts.add("implements " + t);
            return String.join(", ", parts);
        }

        private static void field(String owner, VariableTree v, Tree.Kind kind) {
            var flags = v.getModifiers().getFlags();
            if (kind == Tree.Kind.RECORD && !flags.contains(Modifier.STATIC)) return;
            if (kind == Tree.Kind.ENUM && v.getType() == null) {
                System.out.println("CONST\t" + owner + "\t" + v.getName());
                return;
            }
            if (v.getType() != null && v.getType().toString().isEmpty()) {
                System.out.println("CONST\t" + owner + "\t" + v.getName());
                return;
            }
            String stat = flags.contains(Modifier.STATIC) ? "{static} " : "";
            System.out.println("FIELD\t" + owner + "\t" + stat + vis(flags, kind == Tree.Kind.INTERFACE) + v.getName() + ": " + v.getType());
        }

        private static void method(String owner, MethodTree m, ClassTree type) {
            var flags = m.getModifiers().getFlags();
            boolean ctor = m.getReturnType() == null;
            if (ctor && type.getKind() == Tree.Kind.RECORD && m.getParameters().isEmpty()) return;
            String params = m.getParameters().stream().map(p -> p.getName() + ": " + p.getType()).collect(Collectors.joining(", "));
            String name = ctor ? type.getSimpleName().toString() : m.getName().toString();
            String ret = ctor ? "" : ": " + m.getReturnType();
            String stat = flags.contains(Modifier.STATIC) ? "{static} " : "";
            String abs = type.getKind() == Tree.Kind.INTERFACE && !flags.contains(Modifier.DEFAULT) && !flags.contains(Modifier.STATIC) ? "{abstract} " : "";
            System.out.println("METHOD\t" + owner + "\t" + stat + abs + vis(flags, type.getKind() == Tree.Kind.INTERFACE) + name + "(" + params + ")" + ret);
        }

        private static String vis(Set<Modifier> flags, boolean interfaceMember) {
            if (flags.contains(Modifier.PUBLIC) || interfaceMember) return "+";
            if (flags.contains(Modifier.PRIVATE)) return "-";
            if (flags.contains(Modifier.PROTECTED)) return "#";
            return "~";
        }
    }
}
