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
            if (line.contains("=") && !line.startsWith("if") && !line.startsWith("while")) {
                handleAssignment(line);
            }
            // Handle print statement
            else if (line.startsWith("print(")) {
                handlePrint(line);
            }
            // Handle if statement
            else if (line.startsWith("if ")) {
                boolean conditionResult = handleIfStatement(line);
                int indentationLevel = getIndentationLevel(lines[lineIndex]);
                lineIndex++;

                // Process the block if condition is true
                while (lineIndex < lines.length && getIndentationLevel(lines[lineIndex]) > indentationLevel) {
                    if (conditionResult) {
                        eval(lines[lineIndex]);
                    }
                    lineIndex++;
                }
                continue;
            }
            // Handle while statement
            else if (line.startsWith("while ")) {
                int startLineIndex = lineIndex;
                boolean conditionResult = evaluateCondition(line.substring(6).trim());
                int indentationLevel = getIndentationLevel(lines[lineIndex]);
                lineIndex++;

                while (conditionResult) {
                    int tempLineIndex = lineIndex;

                    while (tempLineIndex < lines.length && getIndentationLevel(lines[tempLineIndex]) > indentationLevel) {
                        eval(lines[tempLineIndex]);
                        tempLineIndex++;
                    }

                    conditionResult = evaluateCondition(line.substring(6).trim());
                }

                // Skip the while block after execution
                while (lineIndex < lines.length && getIndentationLevel(lines[lineIndex]) > indentationLevel) {
                    lineIndex++;
                }
                continue;
            }

            lineIndex++;
        }
    }

    private boolean handleIfStatement(String line) {
        String condition = line.substring(3).trim(); // Remove 'if '
        return evaluateCondition(condition);
    }

    private boolean evaluateCondition(String condition) {
        String[] comparisonOperators = {"<", ">", "==", "<=", ">=", "!="};
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
                String[] mulDivTerms = term.split("(?=[*/])|(?<=[*/])");
                int termResult = 1;
                boolean multiplication = false;
                boolean division = false;

                for (String factor : mulDivTerms) {
                    factor = factor.trim();
                    if (factor.isEmpty()) continue;

                    if (factor.equals("*")) {
                        multiplication = true;
                    } else if (factor.equals("/")) {
                        division = true;
                    } else {
                        int value = Integer.parseInt(factor);
                        if (multiplication) {
                            termResult *= value;
                        } else if (division) {
                            termResult /= value;
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

        String program = """
            a = 12
            b = 5
            while a > 0
                print(a)
                a = a - 1
            print(b)
        """;

        interpreter.eval(program);
    }
}
