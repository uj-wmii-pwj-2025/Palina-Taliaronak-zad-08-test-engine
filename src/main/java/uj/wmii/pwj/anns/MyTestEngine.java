package uj.wmii.pwj.anns;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MyTestEngine {

    private final String className;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please specify test class name");
            System.exit(-1);
        }

        printAsciiArt();
        String className = args[0].trim();
        System.out.printf("Testing class: %s\n", className);
        MyTestEngine engine = new MyTestEngine(className);
        engine.runTests();
    }

    private static void printAsciiArt() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║            MY TEST ENGINE            ║");
        System.out.println("╚══════════════════════════════════════╝");
    }

    public MyTestEngine(String className) {
        this.className = className;
    }

    public void runTests() {
        final Object unit = getObject(className);
        List<Method> testMethods = getTestMethods(unit);

        System.out.printf("\nFound %d test methods:\n", testMethods.size());
        for (int i = 0; i < testMethods.size(); i++) {
            Method m = testMethods.get(i);
            MyTest annotation = m.getAnnotation(MyTest.class);
            if (annotation.params().length > 0) {
                System.out.printf("  %d. %s() with %d parameter set(s)\n",
                        i + 1, m.getName(), annotation.params().length);
            } else {
                System.out.printf("  %d. %s()\n", i + 1, m.getName());
            }
        }
        System.out.println("\n" + "=".repeat(50) + "\n");

        int successCount = 0;
        int failCount = 0;
        int errorCount = 0;

        for (Method m: testMethods) {
            TestResult result = launchSingleMethod(m, unit);
            switch (result) {
                case SUCCESS: successCount++; break;
                case FAIL: failCount++; break;
                case ERROR: errorCount++; break;
            }
        }

        printSummary(testMethods.size(), successCount, failCount, errorCount);
    }

    private TestResult launchSingleMethod(Method m, Object unit) {
        MyTest annotation = m.getAnnotation(MyTest.class);
        String[] params = annotation.params();
        String[] expected = annotation.expected();

        System.out.printf("Running: %s() ", m.getName());

        try {
            if (params.length == 0) {
                Object result = m.invoke(unit);

                if (expected.length > 0) {
                    String actualResult = (result != null) ? result.toString() : "null";
                    if (actualResult.equals(expected[0])) {
                        System.out.println("✓ PASS");
                        return TestResult.SUCCESS;
                    } else {
                        System.out.printf("✗ FAIL (expected: '%s', got: '%s')\n", expected[0], actualResult);
                        return TestResult.FAIL;
                    }
                }

                System.out.println("✓ PASS");
                return TestResult.SUCCESS;

            } else {
                int passed = 0;
                int failed = 0;
                boolean hasError = false;

                for (int i = 0; i < params.length; i++) {
                    try {
                        Object result = m.invoke(unit, params[i]);

                        if (expected.length > i) {
                            String actualResult = (result != null) ? result.toString() : "null";
                            if (actualResult.equals(expected[i])) {
                                passed++;
                            } else {
                                System.out.printf("\n  Parameter '%s': ✗ FAIL (expected: '%s', got: '%s')",
                                        params[i], expected[i], actualResult);
                                failed++;
                            }
                        } else {
                            passed++;
                        }

                    } catch (Exception e) {
                        System.out.printf("\n  Parameter '%s': ⚠ ERROR (%s)",
                                params[i], e.getCause().getClass().getSimpleName());
                        hasError = true;
                        break;
                    }
                }

                if (hasError) {
                    System.out.println();
                    return TestResult.ERROR;
                } else if (failed > 0) {
                    System.out.printf("\n  Result: %d/%d passed\n", passed, params.length);
                    return TestResult.FAIL;
                } else {
                    System.out.printf("\n  Result: %d/%d passed ✓ PASS\n", passed, params.length);
                    return TestResult.SUCCESS;
                }
            }

        } catch (Exception e) {
            System.out.printf("⚠ ERROR (%s)\n", e.getCause().getClass().getSimpleName());
            return TestResult.ERROR;
        }
    }

    private static List<Method> getTestMethods(Object unit) {
        Method[] methods = unit.getClass().getDeclaredMethods();
        return Arrays.stream(methods)
                .filter(m -> m.getAnnotation(MyTest.class) != null)
                .collect(Collectors.toList());
    }

    private static Object getObject(String className) {
        try {
            Class<?> unitClass = Class.forName(className);
            return unitClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return new Object();
        }
    }

    private void printSummary(int total, int success, int fail, int error) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("TEST SUMMARY:");
        System.out.printf("Total tests: %d\n", total);
        System.out.printf("✓ PASS: %d\n", success);
        System.out.printf("✗ FAIL: %d\n", fail);
        System.out.printf("⚠ ERROR: %d\n", error);
        System.out.println("=".repeat(50));
    }
}
