import java.util.HashMap;
import java.util.Map;

public class PythonInterpreter {
    private final Map<String, Integer> variables = new HashMap<>();

    public void eval(String code) {
        String[] lines = code.split("\n");
        int lineIndex = 0;

        while (lineIndex < lines.length) {
            String line = lines[lineIndex].trim();

            if (line.isEmpty()) {
                lineIndex++;
                continue;
            }

            // Handle assignment
            if (line.contains("=") && !line.startsWith("if") && !line.startsWith("elif") && !line.startsWith("while")) {
                handleAssignment(line);
            }
            // Handle print statement
            else if (line.startsWith("print(")) {
                handlePrint(line);
            }
            // Handle if statement
            else if (line.startsWith("if ") && line.endsWith(":")) {
                boolean conditionResult = handleIfStatement(line);
                int indentationLevel = getIndentationLevel(lines[lineIndex]);
                lineIndex++;

                boolean conditionMet = false;
                // Process the block if condition is true
                while (lineIndex < lines.length && getIndentationLevel(lines[lineIndex]) > indentationLevel) {
                    String currentLine = lines[lineIndex].trim();

                    if (currentLine.startsWith("elif") && currentLine.endsWith(":")) {
                        if (!conditionMet) {
                            conditionMet = handleElifStatement(currentLine);
                        }
                    } else if (currentLine.startsWith("else") && currentLine.endsWith(":")) {
                        if (!conditionMet) {
                            evalBlock(lines, lineIndex, indentationLevel);
                            conditionMet = true; // Mark as executed
                        }
                    } else {
                        if (conditionResult && !conditionMet) {
                            evalLine(currentLine); // Execute the block for the matched condition
                            conditionMet = true;
                        }
                    }
                    lineIndex++;
                }
                continue;
            }
            // Handle while statement
            else if (line.startsWith("while ") && line.endsWith(":")) {
                int indentationLevel = getIndentationLevel(lines[lineIndex]);
                boolean conditionResult = evaluateCondition(line.substring(6, line.length() - 1).trim());
                lineIndex++;

                while (conditionResult) {
                    int tempLineIndex = lineIndex;
                    boolean shouldBreak = false;

                    while (tempLineIndex < lines.length && getIndentationLevel(lines[tempLineIndex]) > indentationLevel) {
                        String currentLine = lines[tempLineIndex].trim();
                        if (currentLine.startsWith("break")) {
                            shouldBreak = true;
                            break;
                        }
                        evalLine(currentLine);
                        tempLineIndex++;
                    }

                    if (shouldBreak) {
                        break;
                    }

                    conditionResult = evaluateCondition(line.substring(6, line.length() - 1).trim());
                }

                while (lineIndex < lines.length && getIndentationLevel(lines[lineIndex]) > indentationLevel) {
                    lineIndex++;
                }
                continue;
            }

            lineIndex++;
        }
    }

    private void evalLine(String line) {
        if (line.contains("=")) {
            handleAssignment(line);
        } else if (line.startsWith("print(")) {
            handlePrint(line);
        }
    }

    private void evalBlock(String[] lines, int startLine, int indentationLevel) {
        int lineIndex = startLine + 1;
        while (lineIndex < lines.length && getIndentationLevel(lines[lineIndex]) > indentationLevel) {
            evalLine(lines[lineIndex].trim());
            lineIndex++;
        }
    }

    private boolean handleIfStatement(String line) {
        String condition = line.substring(3, line.length() - 1).trim();
        return evaluateCondition(condition);
    }

    private boolean handleElifStatement(String line) {
        String condition = line.substring(4, line.length() - 1).trim();
        return evaluateCondition(condition);
    }

    private void handleAssignment(String line) {
        String[] parts = line.split("=");
        String varName = parts[0].trim();
        String expression = parts[1].trim();
        int value = evaluateExpression(expression);
        variables.put(varName, value);
    }

    private void handlePrint(String line) {
        String content = line.substring(line.indexOf('(') + 1, line.lastIndexOf(')')).trim();
        int value = evaluateExpression(content);
        System.out.println(value);
    }

    private int evaluateExpression(String expression) {
        while (expression.contains("(")) {
            int startIdx = expression.lastIndexOf("(");
            int endIdx = expression.indexOf(")", startIdx);
            String subExpression = expression.substring(startIdx + 1, endIdx).trim();
            int subResult = evaluateExpression(subExpression);
            expression = expression.substring(0, startIdx) + subResult + expression.substring(endIdx + 1);
        }

        for (String varName : variables.keySet()) {
            expression = expression.replace(varName, String.valueOf(variables.get(varName)));
        }

        String[] addSubTerms = expression.split("(?=[+-])|(?<=[+-])");
        int result = 0;
        int sign = 1;

        for (String term : addSubTerms) {
            term = term.trim();
            if (term.isEmpty()) continue;

            if (term.equals("+")) {
                sign = 1;
            } else if (term.equals("-")) {
                sign = -1;
            } else {
                String[] mulDivModFloorTerms = term.split("(?=[*/%/])|(?<=[*/%/])");
                int termResult = 1;
                boolean multiplication = false;
                boolean division = false;
                boolean modulus = false;
                boolean floorDivision = false;

                for (String factor : mulDivModFloorTerms) {
                    factor = factor.trim();
                    if (factor.isEmpty()) continue;

                    if (factor.equals("*")) {
                        multiplication = true;
                    } else if (factor.equals("/")) {
                        division = true;
                    } else if (factor.equals("%")) {
                        modulus = true;
                    } else if (factor.equals("//")) {
                        floorDivision = true;
                    } else {
                        int value = Integer.parseInt(factor);
                        if (multiplication) {
                            termResult *= value;
                        } else if (division) {
                            termResult /= value;
                        } else if (modulus) {
                            termResult %= value;
                        } else if (floorDivision) {
                            termResult = termResult / value;
                        } else {
                            termResult = value;
                        }
                    }
                }
                result += sign * termResult;
            }
        }
        return result;
    }

    private boolean evaluateCondition(String condition) {
        String[] comparisonOperators = {">=", "<=", ">", "<", "==", "!="};

        for (String operator : comparisonOperators) {
            if (condition.contains(operator)) {
                String[] parts = condition.split(operator);
                int leftValue = evaluateExpression(parts[0].trim());
                int rightValue = evaluateExpression(parts[1].trim());

                switch (operator) {
                    case "<":
                        return leftValue < rightValue;
                    case ">":
                        return leftValue > rightValue;
                    case "==":
                        return leftValue == rightValue;
                    case "<=":
                        return leftValue <= rightValue;
                    case ">=":
                        return leftValue >= rightValue;
                    case "!=":
                        return leftValue != rightValue;
                }
            }
        }
        return false;
    }

    private int getIndentationLevel(String line) {
        int indent = 0;
        while (line.startsWith(" ")) {
            indent++;
            line = line.substring(1);
        }
        return indent;
    }

    public static void main(String[] args) {
        PythonInterpreter interpreter = new PythonInterpreter();
//        String program1 = """
//n = 10
//sum = 0
//i = 1
//while i <= n:
//    sum = sum + i
//    i = i + 1
//print(sum)
//""";
//        interpreter.eval(program1);
//
//        System.out.println("----------------------------");
//
//
//        String program2 = """
//n = 5
//factorial = 1
//i = 1
//while i <= n:
//    factorial = factorial * i
//    i = i + 1
//print(factorial)
//""";
//        interpreter.eval(program2);
//
//        System.out.println("----------------------------");
//
//        String program3 = """
//a = 48
//b = 18
//while b != 0:
//    temp = b
//    b = a % b
//    a = temp
//print(a)
//""";
//        interpreter.eval(program3);
//
//        System.out.println("----------------------------");
//
//        String program4 = """
//n = 1234
//reverse = 0
//while n > 0:
//    digit = n % 10
//    reverse = reverse * 10 + digit
//    n = n // 10
//print(reverse)
//""";
//        interpreter.eval(program4);
//
//        System.out.println("----------------------------");
//
//        String program5 = """
//n = 29
//i = 2
//while i * i <= n:
//    if n % i == 0:
//        print(0)
//        break
//    i = i + 1
//else:
//    print(1)
//""";
//        interpreter.eval(program5);
//
//        System.out.println("----------------------------");
//
//        String program6 = """
//n = 121
//original = n
//reverse = 0
//while n > 0:
//    digit = n % 10
//    reverse = reverse * 10 + digit
//    n = n // 10
//if reverse == original:
//    print(1)
//else:
//    print(0)
//""";
//        interpreter.eval(program6);
//
//        System.out.println("----------------------------");
//
//        String program7 = """
//n = 83756
//largest = 0
//while n > 0:
//    digit = n % 10
//    if digit > largest:
//        largest = digit
//    n = n // 10
//print(largest)
//""";
//        interpreter.eval(program7);
//
//        System.out.println("----------------------------");
//
//        String program8 = """
//n = 12345
//sum = 0
//while n > 0:
//    digit = n % 10
//    sum = sum + digit
//    n = n // 10
//print(sum)
//""";
//        interpreter.eval(program8);
//
//        System.out.println("----------------------------");
//
//        String program9 = """
//n = 7
//i = 1
//while i <= 10:
//    print(n * i)
//    i = i + 1
//""";
//        interpreter.eval(program9);
//
//        System.out.println("----------------------------");
//
//        String program10 = """
//n = 6
//a = 0
//b = 1
//i = 2
//while i < n:
//    temp = a + b
//    a = b
//    b = temp
//    i = i + 1
//print(b)
//""";
//        interpreter.eval(program10);

//        I wouldn't recomend testing multiple inputes at once, because it starts to make errores.

//        1. Sum of First N Numbers                ✔
//        2. Factorial of N                        ✔
//        3. GCD of Two Numbers                    ✔
//        4. Reverse a Number                      ✔
//        5. Check if a Number is Prime            X
//        6. Check if a Number is Palindrome       X
//        7. Find the Largest Digit in a Number    X
//        8. Sum of Digits                         ✔
//        9. Multiplication Table                  ✔
//        10. Nth Fibonacci Number                 ✔









    }
}