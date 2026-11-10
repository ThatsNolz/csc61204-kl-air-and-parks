import com.sun.source.tree.*;
import com.sun.source.util.*;
import java.nio.file.*;
import java.util.*;
import javax.lang.model.element.Modifier;
import javax.tools.*;

/** Parses Java source with the JDK AST and prints a method metrics TSV. */
class JavaQuality {
    public static void main(String[] args) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var files = compiler.getStandardFileManager(null, null, null);
             var paths = Files.walk(Path.of(args[0]))) {
            var sources = paths.filter(p -> p.toString().endsWith(".java")).toList();
            var task = (JavacTask) compiler.getTask(null, files, null, List.of("-proc:none"), null,
                    files.getJavaFileObjectsFromPaths(sources));
            var trees = DocTrees.instance(task);
            System.out.println("file\tmethod\tline\tspan\tcomplexity\tpublic\tdocumented");
            for (var unit : task.parse()) {
                new TreePathScanner<Void,Void>() {
                    @Override public Void visitMethod(MethodTree m, Void unused) {
                        if (m.getBody() == null) return super.visitMethod(m, unused);
                        var loc = trees.getSourcePositions();
                        var start = unit.getLineMap().getLineNumber(loc.getStartPosition(unit, m));
                        var end = unit.getLineMap().getLineNumber(loc.getEndPosition(unit, m) - 1);
                        var ast = new Decisions();
                        ast.scan(m.getBody(), null);
                        var name = Path.of(unit.getSourceFile().toUri()).getFileName();
                        var isPublic = m.getModifiers().getFlags().contains(Modifier.PUBLIC);
                        var documented = trees.getDocComment(getCurrentPath()) != null;
                        System.out.printf("%s\t%s\t%d\t%d\t%d\t%b\t%b%n",
                                name,m.getName(),start,end-start+1,ast.count,isPublic,documented);
                        return super.visitMethod(m, unused);
                    }
                }.scan(unit, null);
            }
        }
    }

    private static final class Decisions extends TreeScanner<Void,Void> {
        int count = 1;
        @Override public Void scan(Tree tree, Void unused) {
            if (tree != null) {
                switch (tree.getKind()) {
                    case IF, FOR_LOOP, ENHANCED_FOR_LOOP, WHILE_LOOP, DO_WHILE_LOOP, CATCH,
                         CONDITIONAL_EXPRESSION, CONDITIONAL_AND, CONDITIONAL_OR -> count++;
                    case CASE -> { if (!((CaseTree) tree).getExpressions().isEmpty()) count++; }
                    case CLASS -> { return null; }
                    default -> { }
                }
            }
            return super.scan(tree, unused);
        }
    }
}
