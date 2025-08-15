package com.moneyflow.moneyflow.controller;

import com.moneyflow.moneyflow.entity.Account;
import com.moneyflow.moneyflow.entity.Category;
import com.moneyflow.moneyflow.entity.Transaction;
import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.enums.TransactionType;
import com.moneyflow.moneyflow.service.AccountService;
import com.moneyflow.moneyflow.service.CategoryService;
import com.moneyflow.moneyflow.service.ChatbotService;
import com.moneyflow.moneyflow.service.TransactionService;
import com.moneyflow.moneyflow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
public class ChatbotController {

    private static final Logger LOGGER = Logger.getLogger(ChatbotController.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    private ChatbotService chatbotService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private UserService userService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private CategoryService categoryService;

    @PostMapping("/api/chatbot")
    public String handleChatbotRequest(@RequestBody Map<String, String> request) throws IOException {
        String userMessage = request.get("message");
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "Vui lòng đăng nhập để sử dụng chatbot.";
        }

        String chatbotResponse = fetchChatbotResponse(userMessage, currentUser);
        if (isNonTransactionResponse(chatbotResponse)) {
            return chatbotResponse;
        }

        return processMultipleTransactions(chatbotResponse, currentUser);
    }

    private String fetchChatbotResponse(String userMessage, User currentUser) throws IOException {
        List<Account> userAccounts = accountService.getAccountsByUser(currentUser);
        List<Category> userCategories = categoryService.findAllCategoriesByUser();
        String walletList = userAccounts.stream().map(Account::getName).collect(Collectors.joining(", "));
        String categoryList = userCategories.stream().map(Category::getName).collect(Collectors.joining(", "));

        String prompt = buildPrompt(walletList, categoryList, userMessage);
        LOGGER.info("Prompt cho Gemini: " + prompt);
        String response = chatbotService.getChatbotResponse(prompt);
        LOGGER.info("Phản hồi từ Gemini: " + response);
        return Normalizer.normalize(response, Normalizer.Form.NFC).toLowerCase().replaceAll("[\\[\\]]", "");
    }

    private String buildPrompt(String walletList, String categoryList, String userMessage) {
        return  "Bạn là Chatbot MoneyFlow, một trang web quản lý tài chính. Bạn sẽ hỗ trợ tôi thêm bớt giao dịch cho trang web. " +
                "Danh sách ví: [" + walletList + "]. " +
                "Danh sách danh mục: [" + categoryList + "]. " +
                "Phân tích câu sau và trích xuất tất cả các giao dịch có trong câu, mỗi giao dịch bao gồm: số tiền, ngày giao dịch (nếu có), ví, danh mục, mô tả (nếu có). " +
                "Với ví, chọn 1 từ danh sách ví, nếu không có trả về 'UnknownWallet'. " +
                "Với danh mục, luôn tìm hoặc tạo một tên danh mục mới dựa trên ngữ cảnh câu, không bao giờ trả về 'UnknownCategory'. " +
                "Xác định loại giao dịch là 'INCOME' nếu ngữ cảnh cho thấy thu tiền (ví dụ: nhận tiền, chuyển vào), " +
                "'EXPENSE' nếu ngữ cảnh cho thấy chi tiền (ví dụ: mua sắm, trả tiền), nếu không rõ thì để 'UNKNOWN'. " +
                "Với mô tả, chỉ bao gồm nội dung trước 'loại' hoặc để trống nếu không có. " +
                "Định dạng trả về: danh sách các giao dịch, mỗi giao dịch trên một dòng với định dạng 'số tiền: số tiền, ngày giao dịch: dd/MM/yyyy, ví: tên ví, danh mục: tên danh mục, mô tả: mô tả, loại: INCOME/EXPENSE/UNKNOWN'. " +
                "Nếu câu không liên quan đến giao dịch, trả lời tự nhiên như một chatbot thông thường. " +
                "Câu: '" + userMessage + "'";
    }

    private boolean isNonTransactionResponse(String response) {
        return !response.contains("số tiền:");
    }

    private String processMultipleTransactions(String response, User currentUser) {
        String[] transactions = response.split("\n");
        StringBuilder result = new StringBuilder();
        for (String transaction : transactions) {
            if (transaction.trim().isEmpty()) continue;

            Transaction parsedTransaction = parseTransaction(transaction, currentUser);
            if (parsedTransaction != null) {
                saveTransaction(parsedTransaction, currentUser);
                appendTransactionResult(result, parsedTransaction);
            }
        }
        return result.toString().trim();
    }

    private Transaction parseTransaction(String transaction, User currentUser) {
        BigDecimal amount = extractAmount(transaction);
        if (amount == null) return null;

        LocalDateTime transactionDate = extractTransactionDate(transaction);
        Account account = extractAccount(transaction, currentUser);
        if (account == null) return null;

        Category category = extractCategory(transaction, currentUser);
        String description = extractDescription(transaction);

        return buildTransaction(amount, transactionDate, account, category, description);
    }

    private BigDecimal extractAmount(String transaction) {
        Pattern pattern = Pattern.compile("số tiền:\\s*([\\d.,]+)\\s*(k|m|vnd|vnđ)?");
        Matcher matcher = pattern.matcher(transaction);
        if (matcher.find()) {
            String raw = matcher.group(1).replaceAll("[.,]", "");
            BigDecimal amount = new BigDecimal(raw);
            String unit = Optional.ofNullable(matcher.group(2)).orElse("vnd");
            if ("k".equals(unit)) amount = amount.multiply(BigDecimal.valueOf(1_000));
            else if ("m".equals(unit)) amount = amount.multiply(BigDecimal.valueOf(1_000_000));
            return amount;
        }
        return null;
    }

    private LocalDateTime extractTransactionDate(String transaction) {
        LocalDateTime date = LocalDateTime.now();
        Pattern pattern = Pattern.compile("ngày giao dịch:\\s*(\\d{2}/\\d{2}/\\d{4})");
        Matcher matcher = pattern.matcher(transaction);
        if (matcher.find()) {
            LocalDate d = LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            date = d.atStartOfDay();
        } else if (transaction.contains("hôm qua")) {
            date = LocalDate.now().minusDays(1).atStartOfDay();
        } else if (transaction.contains("hôm kia")) {
            date = LocalDate.now().minusDays(2).atStartOfDay();
        }
        return date;
    }

    private Account extractAccount(String transaction, User currentUser) {
        Pattern pattern = Pattern.compile("ví:\\s*([^,]+)");
        Matcher matcher = pattern.matcher(transaction);
        String walletName;
        if (matcher.find()) {
            walletName = matcher.group(1).trim();
        } else {
            walletName = "UnknownWallet";
        }
        return accountService.getAccountsByUser(currentUser).stream()
                .filter(a -> a.getName().equalsIgnoreCase(walletName))
                .findFirst().orElse(null);
    }

    private Category extractCategory(String transaction, User currentUser) {
        Pattern pattern = Pattern.compile("danh mục:\\s*([^,]+)");
        Matcher matcher = pattern.matcher(transaction);
        String catName;
        if (matcher.find()) {
            catName = matcher.group(1).trim();
        } else {
            catName = "Chi tiêu khác";
        }
        Optional<Category> catOpt = categoryService.getCategoryByNameAndUser(catName, currentUser);
        return catOpt.orElseGet(() -> {
            Category newCat = new Category();
            newCat.setName(catName);
            Pattern pType = Pattern.compile("loại:\\s*(INCOME|EXPENSE|UNKNOWN)");
            Matcher mType = pType.matcher(transaction);
            if (mType.find()) {
                String typeStr = mType.group(1);
                newCat.setType("INCOME".equals(typeStr) ? TransactionType.INCOME : TransactionType.EXPENSE);
            } else {
                newCat.setType(TransactionType.EXPENSE);
            }
            return categoryService.saveCategory(newCat);
        });
    }

    private String extractDescription(String transaction) {
        Pattern pattern = Pattern.compile("mô tả:\\s*([^,]+)");
        Matcher matcher = pattern.matcher(transaction);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private Transaction buildTransaction(BigDecimal amount, LocalDateTime date, Account account, Category category, String description) {
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setTransactionDate(date);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setType(category.getType());
        transaction.setDescription(description);
        return transaction;
    }

    private void saveTransaction(Transaction transaction, User currentUser) {
        transactionService.createTransaction(transaction, currentUser);
    }

    private void appendTransactionResult(StringBuilder result, Transaction transaction) {
        result.append(String.format("Đã thêm giao dịch: %s, ví %s, danh mục %s, ngày %s%s%n",
                transaction.getAmount(),
                transaction.getAccount().getName(),
                transaction.getCategory().getName(),
                transaction.getTransactionDate().format(DATE_FORMATTER),
                transaction.getDescription().isEmpty() ? "" : ", mô tả: " + transaction.getDescription()));
    }
}