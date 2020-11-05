package net.luckyvalenok.tablefunction;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Stack;
import java.util.TreeMap;

public class Main {
    
    private static final Map<String, Integer> priorities = new HashMap<>();
    private static final Stack<Double> numbers = new Stack<>();
    private static final Stack<String> operations = new Stack<>();
    
    static {
        priorities.put("+", 1);
        priorities.put("-", 1);
        priorities.put("*", 2);
        priorities.put("/", 2);
    }
    
    public static void main(String[] args) throws Exception {
        File input = new File("input.txt");
        if (!input.exists() && input.createNewFile()) {
            System.out.println("Файл был создан. Введите в него следующие данные: начало диапазона, конец диапазона, шаг построения, функцию (используйте x)");
            return;
        }
        FileReader fileReader = new FileReader(input);
        Scanner scanner = new Scanner(fileReader);
        try {
            double start = readDouble(scanner, "начало диапазона");
            double stop = readDouble(scanner, "конец диапазона");
            double step = readDouble(scanner, "шаг построения");
            fileReader.close();
            if (step <= 0) {
                System.out.println("Шаг построения не может быть <= 0");
                return;
            }
            
            String function = readString(scanner, "функцию");
            
            Map<Double, String> doubleMap = new TreeMap<>();
            for (; start <= stop; start += step) {
                doubleMap.put(start, count(function, start));
            }
            
            saveFile(doubleMap);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    }
    
    private static void saveFile(Map<Double, String> map) throws IOException {
        FileWriter output = new FileWriter("output.txt");
        Optional<String> optional = map.values().stream()
            .max(Comparator.comparingInt(aDouble -> String.valueOf(aDouble).length()));
        if (optional.isPresent()) {
            int maxLength = optional.get().length();
            output.write(StringUtils.repeat('-', maxLength * 2 + 3) + "\n");
            output.write(String.format("|%s|%s|\n", StringUtils.center("X", maxLength), StringUtils.center("Y", maxLength)));
            output.write(StringUtils.repeat('-', maxLength * 2 + 3) + "\n");
            for (Map.Entry<Double, String> entry : map.entrySet()) {
                output.write(String.format("|%" + maxLength + "s|%" + maxLength + "s|\n", entry.getKey(), entry.getValue()));
            }
            output.write(StringUtils.repeat('-', maxLength * 2 + 3));
        } else {
            output.write("Программа не смогла произвести подсчеты");
        }
        output.close();
    }
    
    private static String count(String function, double x) {
        return getResult(getBlocks(function.replace("x", x + "")));
    }
    
    private static double readDouble(Scanner scanner, String name) throws Exception {
        hasNext(scanner, name);
        try {
            return Double.parseDouble(scanner.nextLine());
        } catch (NumberFormatException exception) {
            throw new Exception("Не удалось считать " + name + " из файла");
        }
    }
    
    private static String readString(Scanner scanner, String name) throws Exception {
        hasNext(scanner, name);
        return scanner.nextLine();
    }
    
    private static void hasNext(Scanner scanner, String name) throws Exception {
        if (!scanner.hasNext()) {
            throw new Exception("Не удалось считать " + name + " из файла");
        }
    }
    
    public static Stack<String> parseExpression(String expression) {
        Stack<String> sb = new Stack<>();
        Stack<Operator> op = new Stack<>();
        
        char[] chars = expression.toCharArray();
        int N = chars.length;
        
        for (int i = 0; i < N; i++) {
            char ch = chars[i];
            
            if (ch == ' ') {
                continue;
            }
            
            System.out.println(sb.toString() + " " + op.toString());
            
            if (Character.isDigit(ch)) {
                String digit = "";
                boolean hasPoint = false;
                while (Character.isDigit(chars[i]) || (!hasPoint && (chars[i] == '.' || chars[i] == ','))) {
                    if (!hasPoint)
                        hasPoint = chars[i] == '.' || chars[i] == ',';
                    digit += chars[i++];
                }
                i--;
                if (digit.endsWith(".") || digit.endsWith(","))
                    digit = digit.substring(0, digit.length() - 1);
                sb.push(digit);
            } else if (ch == '(') {
                op.push(Operator.OPENING_BRACKET);
            } else if (ch == ')') {
                while (op.peek() != Operator.OPENING_BRACKET) {
                    sb.push(op.pop().getSymbol() + "");
                }
                op.pop();
            } else {
                String operation = "";
                while (Character.isLetter(chars[i]) || Operator.getOperator(chars[i] + "") != null) {
                    operation += chars[i++];
                }
                i--;
                Operator operator = Operator.getOperator(operation);
                if (operator != null) {
                    while (!op.isEmpty() && op.peek().getPriority() >= operator.getPriority()) {
                        sb.push(op.pop().getSymbol() + "");
                    }
                    op.push(operator);
                }
            }
        }
        
        while (!op.isEmpty()) {
            sb.push(op.pop().getSymbol() + "");
        }
        
        return sb;
    }
    
    private static String[] getBlocks(String function) {
        return Arrays.stream(function.trim().split(" "))
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);
    }
    
    private static String getResult(String[] blocks) {
        String stringResult;
        try {
            for (String element : blocks) {
                try {
                    double number = Double.parseDouble(element);
                    numbers.push(number);
                } catch (NumberFormatException exception) {
                    if (priorities.get(element) == null) {
                        throw new Exception("Недопустимая операция " + element);
                    } else {
                        if (!operations.empty()) {
                            int priority = priorities.get(element);
                            
                            String operation;
                            while (!operations.empty() && (operation = operations.peek()) != null && priority <= priorities.get(operation)) {
                                calculate();
                            }
                        }
                        operations.push(element);
                    }
                }
            }
            
            while (!operations.empty()) {
                calculate();
            }
            
            double result = numbers.pop();
            if (numbers.empty()) {
                stringResult = result + ""; // Конкатенация происходит быстрее, чем String.valueOf(result), поэтому сделал так
            } else {
                stringResult = "В примере ошибка. Недостаточно операторов для всех чисел";
            }
        } catch (EmptyStackException exception) {
            stringResult = "В примере ошибка. Недостаточно чисел для всех операторов";
        } catch (Exception exception) {
            stringResult = exception.getMessage();
        }
        
        return stringResult;
    }
    
    private static void calculate() throws Exception {
        String operation = operations.pop();
        Double n2 = numbers.pop();
        Double n1 = numbers.pop();
        double result = 0;
        
        switch (operation) {
            case "+":
                result = n1 + n2;
                break;
            case "-":
                result = n1 - n2;
                break;
            case "*":
                result = n1 * n2;
                break;
            case "/":
                if (n2 == 0) {
                    throw new Exception("Деление на 0 невозможно");
                }
                result = n1 / n2;
                break;
        }
        
        numbers.push(result);
    }
}
