import java.util.HashMap;
import java.util.Map;

public class PythonInterpreter {
    private final Map<String, Integer> variables = new HashMap<>();

    public void eval(String code) {
        String[] lines = code.split("\n");
        boolean inIfBlock = false;
        int indentationLevel = 0;

        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty()) continue;

            // Handle if statement
            if (line.startsWith("if ")) {
                inIfBlock = handleIfStatement(line);
                indentationLevel = line.length() - line.trim().length(); // Update indentation level
            } else if (inIfBlock && getIndentationLevel(line) > indentationLevel) {
                // In an if block, execute lines with greater indentation
                if (line.contains("=")) {
                    handleAssignment(line);
                } else if (line.startsWith("print(")) {
                    handlePrint(line);
                }
            } else if (getIndentationLevel(line) <= indentationLevel) {
                // End of if block when indentation level decreases
                inIfBlock = false;
                if (line.contains("=")) {
                    handleAssignment(line);
                } else if (line.startsWith("print(")) {
                    handlePrint(line);
                }
            }
        }
    }

    private boolean handleIfStatement(String line) {
        String condition = line.substring(3).trim(); // Remove 'if '
        boolean conditionResult = evaluateCondition(condition);
        return conditionResult; // True if condition is non-zero
    }

    private boolean evaluateCondition(String condition) {
        // Evaluate comparison expressions like x < y, x == y, etc.
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
        return false; // Default to false if no comparison is found
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
        int value = evaluateExpression(content); // Evaluate the expression inside print
        System.out.println(value);
    }

    private int evaluateExpression(String expression) {
        // If the expression contains parentheses, evaluate them first
        while (expression.contains("(")) {
            int startIdx = expression.lastIndexOf("(");
            int endIdx = expression.indexOf(")", startIdx);
            String subExpression = expression.substring(startIdx + 1, endIdx).trim();
            int subResult = evaluateExpression(subExpression);
            expression = expression.substring(0, startIdx) + subResult + expression.substring(endIdx + 1);
        }

        // Replace variables with their actual values
        for (String varName : variables.keySet()) {
            expression = expression.replace(varName, String.valueOf(variables.get(varName)));
        }

        // Handle arithmetic operations: +, -, *, /
        String[] addSubTerms = expression.split("(?=[+-])|(?<=[+-])");
        int result = 0;
        int sign = 1; // to handle positive and negative numbers

        for (String term : addSubTerms) {
            term = term.trim();
            if (term.isEmpty()) continue;

            if (term.equals("+")) {
                sign = 1; // Positive sign
            } else if (term.equals("-")) {
                sign = -1; // Negative sign
            } else {
                // Now handle multiplication and division within each term
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

        // Handle comparisons after arithmetic calculations
        if (expression.contains("<") || expression.contains(">") || expression.contains("==") ||
                expression.contains("<=") || expression.contains(">=") || expression.contains("!=")) {
            if (evaluateCondition(expression)) {
                return 1; // Condition is true
            } else {
                return 0; // Condition is false
            }
        }

        return result;
    }

    // A method to check the indentation level of the current line
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

        // Program with nested if statements and more complex expressions
        String program = """
            a = 10
            b = 5
            c = 15
            if a > b
                if c > a
                    d = c - a
                    print(d)
                print(a + b)
            print(c)
        """;

        interpreter.eval(program);
    }
}
