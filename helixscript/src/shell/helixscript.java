package shell;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class helixscript {

    private static final Map<String, Object> variables = new HashMap<>();
    private static final StringBuilder sessionHistory = new StringBuilder();

    public static void main(String[] args) {
        System.out.println("Helixscript v1.0.1");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print(">>> ");
                String input = scanner.nextLine();
                sessionHistory.append(input).append("\n");
                checkShell(input);
            }
        }
    }

    public static void checkShell(String input) {
        input = input.trim();
        if (input.equals("help")) {
            System.out.println("Commands include:\n" +
                    " - exit [exits the shell]\n" +
                    " - println [prints a string or variable]\n" +
                    " - int [declares and assigns an integer variable]\n" +
                    " - string [declares and assigns a string variable]\n" +
                    " - print [prints the value of the specified variable]\n" +
                    " - clear [clears the terminal]\n" +
                    " - math [perform simple mathematical operations]\n" +
                    " - save \"filename.helixscript\" [saves to default directory]\n" +
                    " - save \"filename.helixscript\" \"specified directory\" [saves to the specified directory]");
        } else if (input.startsWith("exit")) {
            System.out.println("Exiting shell.");
            saveSession();
            System.exit(0);
        } else if (input.startsWith("println")) {
            interpretPrintln(input);
        } else if (input.startsWith("int")) {
            interpretIntDeclaration(input);
        } else if (input.startsWith("string")) {
            interpretStringDeclaration(input);
        } else if (input.startsWith("print")) {
            interpretPrint(input);
        } else if (input.equals("clear")) {
            clearScreenAlternative();
        } else if (input.startsWith("math")) {
            evaluateMathExpression(input.substring(4).trim());
        } else if (input.startsWith("save")) {
            saveSession(input);
        } else {
            System.out.println("Invalid input: " + input);
        }
    }

    private static void interpretPrintln(String input) {
        String content = input.substring("println".length()).trim();
        if (content.startsWith("\"") && content.endsWith("\"")) {
            String output = content.substring(1, content.length() - 1);
            System.out.println(output);
            sessionHistory.append("Output: ").append(output).append("\n");
        } else if (variables.containsKey(content)) {
            Object value = variables.get(content);
            System.out.println(value);
            sessionHistory.append("Output: ").append(value).append("\n");
        } else {
            System.out.println("Invalid syntax for println. Use: println \"string\" or println variable");
        }
    }

    private static void interpretIntDeclaration(String input) {
        String[] parts = input.split("\\s*=\\s*");
        if (parts.length == 2) {
            String variableName = parts[0].substring(3).trim();
            try {
                int value = Integer.parseInt(parts[1].trim());
                variables.put(variableName, value);
                System.out.println("Declared and assigned integer variable '" + variableName + "' with value: " + value);
                sessionHistory.append("Declaration: ").append("int ").append(variableName).append(" = ").append(value).append("\n");
            } catch (NumberFormatException e) {
                System.out.println("Invalid integer value.");
            }
        } else {
            System.out.println("Invalid int declaration.");
        }
    }

    private static void interpretStringDeclaration(String input) {
        String[] parts = input.split("\\s*=\\s*");
        if (parts.length == 2) {
            String variableName = parts[0].substring(6).trim();
            String value = parts[1].replaceAll(".*\"(.*)\".*", "$1");
            variables.put(variableName, value);
            System.out.println("Declared and assigned string variable '" + variableName + "' with value: " + value);
            sessionHistory.append("Declaration: ").append("string ").append(variableName).append(" = \"").append(value).append("\"\n");
        } else {
            System.out.println("Invalid string declaration.");
        }
    }

    private static void interpretPrint(String input) {
        String variableName = input.split("\\s+")[1].trim();
        if (variables.containsKey(variableName)) {
            Object value = variables.get(variableName);
            System.out.println(value);
            sessionHistory.append("Output: ").append(value).append("\n");
        } else {
            System.out.println("Variable '" + variableName + "' not found.");
        }
    }

    private static void clearScreenAlternative() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    private static void evaluateMathExpression(String expression) {
        try {
            double result = evaluateExpression(expression);
            System.out.println("Result: " + result);
            sessionHistory.append("Output: ").append(result).append("\n");
        } catch (Exception e) {
            System.out.println("Error evaluating the expression.");
        }
    }

    private static double evaluateExpression(String expression) {
        expression = expression.replaceAll("\\s", "");
        return parseExpression(expression);
    }

    private static double parseExpression(String expression) {
        ExpressionParser parser = new ExpressionParser(expression);
        return parser.parse();
    }

    private static class ExpressionParser {
        private final String expression;
        private int pos;
        private int ch;

        public ExpressionParser(String expression) {
            this.expression = expression;
            this.pos = -1;
            this.ch = -1;
            nextChar();
        }

        private void nextChar() {
            ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
        }

        private boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        public double parse() {
            double x = parseExpression();
            if (pos < expression.length()) throw new RuntimeException("Unexpected: " + (char) ch);
            return x;
        }

        private double parseExpression() {
            double x = parseTerm();
            for (;;) {
                if (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }

        private double parseTerm() {
            double x = parseFactor();
            for (;;) {
                if (eat('*')) x *= parseFactor();
                else if (eat('/')) x /= parseFactor();
                else return x;
            }
        }

        private double parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -parseFactor();

            double x;
            int startPos = pos;
            if (eat('(')) {
                x = parseExpression();
                eat(')');
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Double.parseDouble(expression.substring(startPos, pos));
            } else {
                throw new RuntimeException("Unexpected: " + (char) ch);
            }

            if (eat('^')) x = Math.pow(x, parseFactor());

            return x;
        }
    }

    private static void saveSession() {
        saveSession("default");
    }

    private static void saveSession(String input) {
        String fileName = extractFileName(input);
        saveToFile(fileName, "c:\\users\\user\\documents\\helixscript\\");
    }

    private static void saveToFile(String fileName, String directory) {
        String userDocumentsDir = System.getProperty("user.home") + "\\Documents\\helixscript\\";
        String fullPath = directory.startsWith("\\") ? directory + fileName : userDocumentsDir + fileName;

        File dir = new File(fullPath).getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            System.out.println("Error creating directory: " + dir.getAbsolutePath());
            return;
        }

        File file = new File(fullPath);
        if (file.exists()) {
            System.out.println("Error: File '" + fullPath + "' already exists.");
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(fullPath))) {
            String[] lines = sessionHistory.toString().split("\\n");
            for (String line : lines) {
                // Remove the '>' character at the beginning of each line
                line = line.startsWith(">>>") ? line.substring(4) : line;
                writer.println(line);
            }
            System.out.println("Session saved to " + fullPath);
        } catch (IOException e) {
            System.out.println("Error saving session: " + e.getMessage());
        }
    }

    private static String extractFileName(String input) {
        String[] parts = input.split("\\s+");
        return (parts.length >= 2) ? parts[1] : "helixfile.helixscript";
    }
}
