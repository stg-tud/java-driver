package bblfsh;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * GoGen generates Go code with constant definitions for UAST conversion.
 */
public class GoGen {

    @Test
    public void generate() {
        final PrintStream out = System.out;

        final List<Class<? extends ASTNode>> types = concreteSubTypesOf(ASTNode.class);
        types.sort(Comparator.comparing(Class::getCanonicalName));

        final List<String> structuralProperties = structuralPropertyNamesOf(types)
                .stream()
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        final List<String> keywords = keywords();

        out.print(""
                + "// Package jdt defines constants from Eclipse JDT AST.\n"
                + "package jdt\n"
                + "// GENERATED BY java-driver GoGen\n"
                + "// DO NOT EDIT\n"
                + "\n"
                + "import \"gopkg.in/bblfsh/sdk.v0/uast/ann\"\n\n"
        );
        out.print(""
                + "// Eclipse JDT node types.\n"
                + "// This includes all non-abstract classes extending from ASTNode.\n"
                + "// See http://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Freference%2Fapi%2Findex.html&overview-summary.html\n"
                + "var (\n"
        );
        for (final Class<? extends ASTNode> type : types) {
            out.printf("\t%s = ann.HasInternalType(\"%s\")\n", type.getSimpleName(), type.getSimpleName());
        }
        out.print(")\n");
        out.print("\n");
        out.print(""
                + "// Eclipse JDT structural properties IDs.\n"
                + "var (\n"
        );
        for (final String prop : structuralProperties) {
            final String varName = "Property" + StringUtils.capitalize(prop);
            out.printf("\t%s = ann.HasInternalRole(\"%s\")\n", varName, prop);
        }
        out.print(")\n");
        out.print("\n");
        out.print(""
                + "// Java Keywords\n"
                + "var (\n"
        );
        for (final String kw : keywords) {
            final String varName = "Keyword" + StringUtils.capitalize(kw.toLowerCase(Locale.ENGLISH));
            out.printf("\t%s = ann.HasToken(\"%s\")\n", varName, kw);
        }
        out.print(")\n");
    }

    private static List<String> structuralPropertyNamesOf(final List<Class<? extends ASTNode>> nodes) {
        final List<String> names = new ArrayList<>();
        for (final Class<? extends ASTNode> node : nodes) {
            try {
                final Method m = node.getDeclaredMethod("propertyDescriptors", int.class);
                final List l = (List) m.invoke(null, AST.JLS8);
                for (final Object o : l) {
                    final StructuralPropertyDescriptor d = (StructuralPropertyDescriptor) o;
                    names.add(d.getId());
                }
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
                throw new RuntimeException("unexpected exception", ex);
            }
        }
        return names;
    }

    private static <T> List<Class<? extends T>> concreteSubTypesOf(final Class<? extends T> clazz) {
        final ConfigurationBuilder conf = new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forClass(clazz))
                .setScanners(new SubTypesScanner(true));
        final Reflections reflections = new Reflections(conf);
        final List<Class<? extends T>> result = new ArrayList<>();
        for (final Class<? extends T> type : reflections.getSubTypesOf(clazz)) {
            if (type.isInterface()) {
                continue;
            }

            if (Modifier.isAbstract(type.getModifiers())) {
                continue;
            }

            result.add(type);
        }

        return result;
    }

    private static List<String> keywords() {
        final Field[] fields = org.eclipse.jdt.internal.codeassist.impl.Keywords.class
                .getFields();
        final List<String> kws = new ArrayList<>();
        for (final Field field : fields) {
            if (!char[].class.isAssignableFrom(field.getType())) {
                continue;
            }

            try {
                final char[] kw = (char[]) field.get(null);
                kws.add(new String(kw));
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
        return kws;
    }
}
