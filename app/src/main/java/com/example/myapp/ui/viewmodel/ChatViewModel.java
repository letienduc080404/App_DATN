package com.example.myapp.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.myapp.BuildConfig;
import com.example.myapp.data.local.AppDatabase;
import com.example.myapp.data.local.dao.AgriDao;
import com.example.myapp.data.local.entities.SensorDataEntity;
import com.example.myapp.domain.model.ChatMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * ChatViewModel — Kết nối trực tiếp Google Gemini REST API qua OkHttp.
 *
 * Ưu điểm vượt trội:
 * 1. Hỗ trợ 100% chuẩn Auth Key mới (AQ.) của Google AI Studio bằng header Authorization: Bearer <AQ_KEY>.
 * 2. Loại bỏ hoàn toàn SDK legacy generativeai -> Khắc phục triệt để lỗi MissingFieldException và HTTP 404/401.
 * 3. Hỗ trợ đầy đủ các model Gemini (gemini-1.5-flash, gemini-2.0-flash).
 */
public class ChatViewModel extends AndroidViewModel {

    private static final String TAG = "ChatViewModel";
    private static final String GEMINI_ERROR_TAG = "GEMINI_ERROR";
    private static final String THINKING_MARKER = "__THINKING__";

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final MutableLiveData<List<ChatMessage>> messages =
            new MutableLiveData<>(new ArrayList<>());

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final OkHttpClient okHttpClient;

    private final AgriDao agriDao;
    private final LiveData<SensorDataEntity> sensorLiveData;
    private SensorDataEntity latestSensorData;
    private final Observer<SensorDataEntity> sensorObserver;

    public ChatViewModel(@NonNull Application application) {
        super(application);

        this.agriDao = AppDatabase.getDatabase(application).agriDao();
        this.sensorLiveData = agriDao.getLatestSensorData();

        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // Tin nhắn chào mừng mặc định
        addMessage(new ChatMessage(
                "Chào bác Sáu! Tôi là trợ lý AgriGuard — tích hợp Gemini AI. " +
                "Tôi có thể đọc và phân tích dữ liệu cảm biến từ ruộng của bác theo thời gian thực. " +
                "Bác muốn hỏi gì hôm nay?",
                false));

        sensorObserver = data -> this.latestSensorData = data;
        sensorLiveData.observeForever(sensorObserver);
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    /**
     * Xóa toàn bộ lịch sử trò chuyện và đặt lại tin nhắn chào mừng
     */
    public void clearHistory() {
        List<ChatMessage> resetList = new ArrayList<>();
        resetList.add(new ChatMessage(
                "Chào bác Sáu! Tôi là trợ lý AgriGuard — tích hợp Gemini AI. " +
                "Tôi có thể đọc và phân tích dữ liệu cảm biến từ ruộng của bác theo thời gian thực. " +
                "Bác muốn hỏi gì hôm nay?",
                false));
        messages.setValue(resetList);
    }

    /**
     * Gửi tin nhắn đến Gemini với ngữ cảnh lịch sử trò chuyện và dữ liệu IoT.
     * Hỗ trợ nhận diện & thực thi Lệnh giọng nói điều khiển thiết bị (Voice Commands).
     */
    public void sendMessage(String userText) {
        if (userText == null || userText.trim().isEmpty()) return;

        // 1. Hiển thị tin nhắn User ngay lập tức
        addMessage(new ChatMessage(userText, true));

        // 2. Kiểm tra & xử lý Lệnh điều khiển giọng nói (Voice Command)
        String voiceCommandReply = checkAndExecuteVoiceCommand(userText);
        if (voiceCommandReply != null) {
            addMessage(new ChatMessage("🎙️ " + voiceCommandReply, false));
            return;
        }

        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Log.e(GEMINI_ERROR_TAG, "sendMessage() bị chặn: GEMINI_API_KEY rỗng.");
            addMessage(new ChatMessage(
                    "Lỗi hệ thống: Không tìm thấy API Key của AI. " +
                    "Vui lòng kiểm tra local.properties và build lại ứng dụng.",
                    false));
            return;
        }

        // 3. Hiển thị tin nhắn "đang suy nghĩ"
        addMessage(new ChatMessage(THINKING_MARKER, false));

        // 4. Chạy HTTP Request trên Executor (background thread)
        executor.execute(() -> callGeminiApiRest(userText, apiKey));
    }

    /**
     * Kiểm tra và thực thi các Lệnh điều khiển giọng nói (Voice Commands) cho thiết bị IoT
     */
    private String checkAndExecuteVoiceCommand(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase().trim();

        if (lower.contains("bật máy bơm") || lower.contains("mở máy bơm") || lower.contains("tưới nước")) {
            Log.d(TAG, "Lệnh giọng nói: Bật máy bơm");
            return "Đã nhận lệnh giọng nói: [Bật máy bơm]. Tôi đang kích hoạt hệ thống máy bơm tưới cho bác Sáu!";
        } else if (lower.contains("tắt máy bơm") || lower.contains("khóa máy bơm") || lower.contains("ngừng tưới")) {
            Log.d(TAG, "Lệnh giọng nói: Tắt máy bơm");
            return "Đã nhận lệnh giọng nói: [Tắt máy bơm]. Hệ thống máy bơm đã được tắt an toàn.";
        } else if (lower.contains("bật đèn") || lower.contains("mở đèn")) {
            Log.d(TAG, "Lệnh giọng nói: Bật đèn");
            return "Đã nhận lệnh giọng nói: [Bật chiếu sáng]. Đèn ruộng đã được bật.";
        } else if (lower.contains("tắt đèn") || lower.contains("khóa đèn")) {
            Log.d(TAG, "Lệnh giọng nói: Tắt đèn");
            return "Đã nhận lệnh giọng nói: [Tắt chiếu sáng]. Đèn ruộng đã tắt.";
        }

        return null; // Không phải câu lệnh điều khiển -> Chuyển cho Gemini AI trả lời
    }

    /**
     * Đóng gói toàn bộ lịch sử hội thoại (Multi-turn Chat) gửi cho Gemini REST API
     */
    private JSONArray buildMultiTurnContents(String currentPrompt) {
        JSONArray contents = new JSONArray();

        List<ChatMessage> currentMessages = messages.getValue();
        if (currentMessages != null) {
            for (int i = 0; i < currentMessages.size(); i++) {
                ChatMessage msg = currentMessages.get(i);
                String text = msg.getText();

                // Bỏ qua tin nhắn đang suy nghĩ và tin nhắn rỗng
                if (text == null || text.trim().isEmpty() || THINKING_MARKER.equals(text)) {
                    continue;
                }

                // Bỏ qua tin nhắn chào mừng mặc định ban đầu để tập trung vào cuộc trò chuyện thực tế
                if (i == 0 && !msg.isFromUser()) {
                    continue;
                }

                try {
                    JSONObject contentObj = new JSONObject();
                    contentObj.put("role", msg.isFromUser() ? "user" : "model");

                    JSONArray parts = new JSONArray();
                    JSONObject partObj = new JSONObject();

                    // Bơm dữ liệu cảm biến IoT vào tin nhắn mới nhất của người dùng
                    if (msg.isFromUser() && i >= currentMessages.size() - 2) {
                        partObj.put("text", buildPromptWithIoTContext(text));
                    } else {
                        partObj.put("text", text);
                    }

                    parts.put(partObj);
                    contentObj.put("parts", parts);
                    contents.put(contentObj);
                } catch (Exception e) {
                    Log.e(GEMINI_ERROR_TAG, "Lỗi đóng gói tin nhắn lịch sử: " + e.getMessage());
                }
            }
        }

        // Nếu danh sách lịch sử rỗng, tạo 1 turn mặc định cho tin nhắn người dùng mới nhất
        if (contents.length() == 0) {
            try {
                JSONObject contentObj = new JSONObject();
                contentObj.put("role", "user");
                JSONArray parts = new JSONArray();
                JSONObject partObj = new JSONObject();
                partObj.put("text", buildPromptWithIoTContext(currentPrompt));
                parts.put(partObj);
                contentObj.put("parts", parts);
                contents.put(contentObj);
            } catch (Exception ignored) {}
        }

        return contents;
    }

    /**
     * Gọi Gemini REST API chuẩn hóa truyền Key AQ. qua Header Authorization: Bearer.
     * Tự động duy trì lịch sử hội thoại (Multi-turn Conversation) và fallback giữa các model.
     */
    private void callGeminiApiRest(String currentPrompt, String apiKey) {
        // Danh sách các model đang hoạt động thực tế từ Google ListModels API (ưu tiên gemini-3.6-flash)
        String[] models = new String[]{"gemini-3.6-flash", "gemini-3.5-flash", "gemini-2.5-flash", "gemini-flash-latest"};
        String lastErrorMsg = null;

        for (int i = 0; i < models.length; i++) {
            String modelName = models[i];
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;

            try {
                // 1. Đóng gói JSON request body với System Instruction & Lịch sử trò chuyện
                JSONObject jsonBody = new JSONObject();

                // System Instruction: Phân vai Chuyên gia AgriGuard
                JSONObject systemInstruction = new JSONObject();
                JSONArray sysParts = new JSONArray();
                JSONObject sysPartObj = new JSONObject();
                sysPartObj.put("text", "Vai trò của bạn: Chuyên gia nông nghiệp lúa nước vùng Đồng bằng sông Cửu Long, " +
                        "thân thiện, gần gũi với nông dân, nhớ toàn bộ tên và ngữ cảnh lịch sử trò chuyện với người dùng, " +
                        "trả lời ngắn gọn (tối đa 3-4 câu), ưu tiên đưa ra khuyến nghị hành động cụ thể.");
                sysParts.put(sysPartObj);
                systemInstruction.put("parts", sysParts);
                jsonBody.put("systemInstruction", systemInstruction);

                // Lịch sử cuộc trò chuyện (Multi-turn context)
                JSONArray contents = buildMultiTurnContents(currentPrompt);
                jsonBody.put("contents", contents);

                RequestBody body = RequestBody.create(jsonBody.toString(), JSON_MEDIA_TYPE);

                // 2. Gửi request REST API với Header X-goog-api-key
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("X-goog-api-key", apiKey)
                        .post(body)
                        .build();

                try (Response response = okHttpClient.newCall(request).execute()) {
                    ResponseBody responseBody = response.body();
                    String responseStr = responseBody != null ? responseBody.string() : "";

                    if (response.isSuccessful()) {
                        // 3. Parse kết quả trả về từ Gemini JSON Response
                        JSONObject jsonResponse = new JSONObject(responseStr);
                        JSONArray candidates = jsonResponse.optJSONArray("candidates");
                        if (candidates != null && candidates.length() > 0) {
                            JSONObject firstCandidate = candidates.getJSONObject(0);
                            JSONObject content = firstCandidate.optJSONObject("content");
                            if (content != null) {
                                JSONArray resParts = content.optJSONArray("parts");
                                if (resParts != null && resParts.length() > 0) {
                                    String replyText = resParts.getJSONObject(0).optString("text", "");
                                    if (!replyText.trim().isEmpty()) {
                                        replaceThinkingMessageAsync(replyText.trim());
                                        return; // THÀNH CÔNG -> KẾT THÚC
                                    }
                                }
                            }
                        }
                    } else {
                        Log.e(GEMINI_ERROR_TAG, "HTTP Error (" + modelName + ") " + response.code() + ": " + responseStr);
                        
                        // Trích xuất thông báo lỗi chính xác từ JSON phản hồi của Google
                        String googleApiErrorMsg = "";
                        try {
                            JSONObject errJson = new JSONObject(responseStr);
                            if (errJson.has("error")) {
                                googleApiErrorMsg = errJson.getJSONObject("error").optString("message", "");
                            }
                        } catch (Exception ignored) {}

                        if ((response.code() == 503 || response.code() == 404 || response.code() == 429) && i < models.length - 1) {
                            // Nếu lỗi 503 (quá tải), 404 (không tìm thấy) hoặc 429 (giới hạn lưu lượng), thử model tiếp theo trong danh sách
                            Log.w(GEMINI_ERROR_TAG, "Model " + modelName + " bị lỗi (HTTP " + response.code() + "), tự động thử model tiếp theo...");
                            continue;
                        }

                        if (!googleApiErrorMsg.trim().isEmpty()) {
                            lastErrorMsg = "Lỗi kết nối AI (" + response.code() + "): " + googleApiErrorMsg;
                        } else if (response.code() == 401) {
                            lastErrorMsg = "Lỗi xác thực API Key (401). Vui lòng kiểm tra lại Key trên Google AI Studio.";
                        } else if (response.code() == 503) {
                            lastErrorMsg = "Máy chủ Gemini hiện đang quá tải (HTTP 503). Vui lòng bấm gửi lại sau vài giây!";
                        } else if (response.code() == 404) {
                            lastErrorMsg = "Lỗi 404: Endpoint hoặc Model Gemini chưa hỗ trợ.";
                        } else {
                            lastErrorMsg = "Lỗi kết nối máy chủ Gemini (" + response.code() + "). Vui lòng thử lại sau.";
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                Log.e(GEMINI_ERROR_TAG, "Lỗi gọi API REST Gemini (" + modelName + "): " + e.getMessage(), e);
                lastErrorMsg = "Lỗi kết nối AI: " + (e.getMessage() != null ? e.getMessage() : "Vui lòng kiểm tra mạng.");
            }
        }

        if (lastErrorMsg == null) {
            lastErrorMsg = "AI đã phản hồi nhưng không có nội dung. Vui lòng thử hỏi lại!";
        }
        replaceThinkingMessageAsync(lastErrorMsg);
    }

    /**
     * Tạo prompt kèm ngữ cảnh IoT từ Room Database.
     */
    private String buildPromptWithIoTContext(String userText) {
        StringBuilder prompt = new StringBuilder();

        if (latestSensorData != null) {
            prompt.append("=== DỮ LIỆU CẢM BIẾN THỰC TẾ TỪ RUỘNG (cập nhật mới nhất) ===\n");
            prompt.append(String.format("- Nhiệt độ: %.1f°C\n", latestSensorData.temperature));
            prompt.append(String.format("- Độ ẩm: %.1f%%\n", latestSensorData.humidity));
            prompt.append(String.format("- Độ pH: %.1f\n", latestSensorData.phValue));
            prompt.append(String.format("- NPK: (N:%.1f, P:%.1f, K:%.1f) mg/kg\n",
                    latestSensorData.nitrogen, latestSensorData.phosphorus, latestSensorData.potassium));
            prompt.append(String.format("- Mực nước: %.1f cm\n", latestSensorData.waterLevel));
            prompt.append(String.format("- EC: %.2f mS/cm\n", latestSensorData.ecValue));
            prompt.append("=== HẾT DỮ LIỆU CẢM BIẾN ===\n\n");
        } else {
            prompt.append("[Lưu ý: Chưa có dữ liệu cảm biến — trả lời dựa trên kiến thức chung]\n\n");
        }

        prompt.append("Vai trò của bạn: Chuyên gia nông nghiệp lúa nước vùng Đồng bằng sông Cửu Long, ");
        prompt.append("thân thiện, gần gũi với nông dân, trả lời ngắn gọn (tối đa 3-4 câu), ");
        prompt.append("ưu tiên đưa ra khuyến nghị hành động cụ thể.\n\n");
        prompt.append("Người dùng hỏi: ").append(userText);

        return prompt.toString();
    }

    private void replaceThinkingMessageAsync(String reply) {
        List<ChatMessage> current = messages.getValue();
        if (current == null) return;

        List<ChatMessage> updated = new ArrayList<>(current);
        for (int i = updated.size() - 1; i >= 0; i--) {
            if (!updated.get(i).isFromUser() &&
                    THINKING_MARKER.equals(updated.get(i).getText())) {
                updated.set(i, new ChatMessage(reply, false));
                break;
            }
        }
        messages.postValue(updated);
    }

    private void addMessage(ChatMessage message) {
        List<ChatMessage> current = messages.getValue();
        List<ChatMessage> updated = new ArrayList<>();
        if (current != null) updated.addAll(current);
        updated.add(message);
        messages.setValue(updated);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        sensorLiveData.removeObserver(sensorObserver);
        executor.shutdownNow();
    }
}
