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
import java.time.LocalDate;
import java.time.LocalDateTime;
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

        // 1. Lấy danh sách ví và danh mục để gửi cho Chatbot
        List<Account> userAccounts = accountService.getAccountsByUser(currentUser);
        String walletList = userAccounts.stream()
                .map(Account::getName)
                .collect(Collectors.joining(", "));
        List<Category> userCats = categoryService.findAllCategoriesByUser();
        String catList = userCats.stream()
                .map(Category::getName)
                .collect(Collectors.joining(", "));

        String prompt = "Danh sách ví: [" + walletList + "]. " +
                "Danh sách danh mục: [" + catList + "]. " +
                "Phân tích câu sau và trích xuất: số tiền, ngày giao dịch (nếu có), ví, danh mục, mô tả (nếu có). " +
                "Với ví, chọn 1 từ danh sách ví, nếu không có trả về 'UnknownWallet'. " +
                "Với danh mục, chọn 1 từ danh sách danh mục, nếu không có trả về tên mới. " +
                "Định dạng trả về: 'số tiền: [số tiền], ngày giao dịch: [dd/MM/yyyy], ví: [tên ví], danh mục: [tên danh mục], mô tả: [mô tả]'. " +
                "Câu: '" + userMessage + "'";
        LOGGER.info("Prompt cho Gemini: " + prompt);
        String chatbotResponse = chatbotService.getChatbotResponse(prompt);
        LOGGER.info("Phản hồi từ Gemini: " + chatbotResponse);

        // 2. Chuẩn hóa
        String resp = Normalizer.normalize(chatbotResponse, Normalizer.Form.NFC).toLowerCase();
        resp = resp.replaceAll("[\\[\\]]","");

        // 3. Parse số tiền
        Pattern pAmt = Pattern.compile("số tiền:\\s*([\\d.,]+)\\s*(k|m|vnd|vnđ)?");
        Matcher mAmt = pAmt.matcher(resp);
        BigDecimal amount;
        if (mAmt.find()) {
            String raw = mAmt.group(1).replaceAll("[.,]", "");
            amount = new BigDecimal(raw);
            String unit = Optional.ofNullable(mAmt.group(2)).orElse("vnd");
            if ("k".equals(unit)) amount = amount.multiply(BigDecimal.valueOf(1_000));
            else if ("m".equals(unit)) amount = amount.multiply(BigDecimal.valueOf(1_000_000));
        } else {
            return "Không phân tích được số tiền.";
        }

        // 4. Parse ngày
        LocalDateTime transactionDate = LocalDateTime.now();
        if (resp.contains("hôm qua")) {
            transactionDate = LocalDate.now().minusDays(1).atStartOfDay();
        } else if (resp.contains("hôm kia")) {
            transactionDate = LocalDate.now().minusDays(2).atStartOfDay();
        } else {
            Pattern pDate = Pattern.compile("ngày giao dịch:\\s*(\\d{2}/\\d{2}/\\d{4})");
            Matcher mDate = pDate.matcher(resp);
            if (mDate.find()) {
                LocalDate d = LocalDate.parse(mDate.group(1), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                transactionDate = d.atStartOfDay();
            }
        }

        // 5. Parse ví
        Pattern pWallet = Pattern.compile("ví:\\s*([^,]+)");
        Matcher mWallet = pWallet.matcher(resp);
        if (!mWallet.find()) {
            return "Thiếu thông tin ví.";
        }
        String walletName = mWallet.group(1).trim();

        // So sánh không phân biệt in hoa/in thường
        Optional<Account> accountOpt = userAccounts.stream()
                .filter(a -> a.getName().equalsIgnoreCase(walletName))
                .findFirst();

        if (!accountOpt.isPresent()) {
            return "Ví này chưa tồn tại";
        }
        Account account = accountOpt.get();

        // 6. Parse danh mục & tạo mới nếu cần
        Pattern pCat = Pattern.compile("danh mục:\\s*([^,]+)");
        Matcher mCat = pCat.matcher(resp);
        if (!mCat.find()) {
            return "Thiếu thông tin danh mục.";
        }
        String catName = mCat.group(1).trim();
        final String respFinal = resp;
        Optional<Category> catOpt = categoryService.getCategoryByNameAndUser(catName, currentUser);
        Category category = catOpt.orElseGet(() -> {
            Category newCat = new Category();
            newCat.setName(catName);
            if (respFinal.contains("nhận") || respFinal.contains("lương")) newCat.setType(TransactionType.INCOME);
            else newCat.setType(TransactionType.EXPENSE);
            return categoryService.saveCategory(newCat);
        });

        // 7. Parse mô tả
        Pattern pDesc = Pattern.compile("mô tả:\\s*(.+)");
        Matcher mDesc = pDesc.matcher(resp);
        String description = mDesc.find() ? mDesc.group(1).trim() : null;

        // 8. Tạo và lưu giao dịch
        Transaction txn = new Transaction();
        txn.setAmount(amount);
        txn.setTransactionDate(transactionDate);
        txn.setAccount(account);
        txn.setCategory(category);
        txn.setType(category.getType());
        txn.setDescription(description);
        transactionService.createTransaction(txn, currentUser);

        return String.format("Đã thêm giao dịch: %s, ví %s, danh mục %s, ngày %s%s",
                amount,
                walletName,
                category.getName(),
                transactionDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                description != null ? ", mô tả: " + description : "");
    }
}
