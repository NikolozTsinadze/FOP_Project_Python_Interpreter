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
            else if (line.startsWith("if ")) {
                boolean conditionResult = handleIfStatement(line);
                int indentationLevel = getIndentationLevel(lines[lineIndex]);
                lineIndex++;

                // Process the block if condition is true
                boolean skipElse = false;
                boolean skipElif = false;

                while (lineIndex < lines.length && getIndentationLevel(lines[lineIndex]) > indentationLevel) {
                    String currentLine = lines[lineIndex].trim();

                    if (currentLine.startsWith("elif")) {
                        if (!conditionResult && !skipElif) {
                            conditionResult = handleIfStatement(currentLine);
                            skipElif = true;
                        }
                    } else if (currentLine.startsWith("else")) {
                        if (conditionResult) {
                            skipElse = true;
                        } else {
                            evalLine(currentLine);
                        }
                    } else {
                        if (!skipElse) {
                            if (conditionResult) {
                                evalLine(currentLine);
                            }
                        }
                    }
                    lineIndex++;
                }
                continue;
            }
            // Handle while statement
            else if (line.startsWith("while ")) {
                int indentationLevel = getIndentationLevel(lines[lineIndex]);
                boolean conditionResult = evaluateCondition(line.substring(6).trim());
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

                    conditionResult = evaluateCondition(line.substring(6).trim());
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

    private boolean handleIfStatement(String line) {
        String condition = line.substring(3).trim();
        return evaluateCondition(condition);
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
            num = 30
            result = num
            
            if num <= 1
                result = 0
            else
                i = 2
                while i * i <= num
                    if num % i == 0
                        result = 0
                        break
                    i = i + 1
            
            print(result)
        """;

        interpreter.eval(program);
    }
}
//        In this patch, we introduced else and elif statements.
//        Additionally, the code has been updated to handle all arithmetic operations,
//        including addition (+), subtraction (-), multiplication (*), division (/), modulo (%), and floor division (//).
