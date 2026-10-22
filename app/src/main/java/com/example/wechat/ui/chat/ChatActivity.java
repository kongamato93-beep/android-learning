package com.example.wechat.ui.chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wechat.R;
import com.example.wechat.adapter.MessageAdapter;
import com.example.wechat.ai.AiError;
import com.example.wechat.ai.AiReplyManager;
import com.example.wechat.ai.AiRequest;
import com.example.wechat.data.DataProvider;
import com.example.wechat.data.DataRepository;
import com.example.wechat.data.WriteFailureListener;
import com.example.wechat.model.Contact;
import com.example.wechat.model.ContactType;
import com.example.wechat.model.Conversation;
import com.example.wechat.model.Message;
import com.example.wechat.util.InsetsHelper;

import java.util.Arrays;
import java.util.List;

/**
 * 聊天窗口。
 *
 * 发送链路刻意收敛成 {@link #sendText} 一个入口：
 * 现在只是把消息追加进本地列表，将来要接 Agent，
 * 只需在这个方法里先把文本交给消息处理器，再落库，界面不用改。
 */
public class ChatActivity extends AppCompatActivity
        implements View.OnClickListener, WriteFailureListener {

    private static final String TAG = "ChatActivity";

    public static final String EXTRA_CONVERSATION_ID = "extra_conversation_id";

    private final DataRepository repository = DataProvider.get();

    private String conversationId;
    private int debugSeq = 0;
    private MessageAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private View rootView;
    /** 最近一次滚动后的位置状态：列表是不是停在最后一条上 */
    private boolean lastKnownAtBottom = true;
    private EditText etInput;
    private TextView tvSend;
    private ImageView ivPlus;

    /** 对方是不是 AI 联系人：决定发完消息要不要去要回复 */
    private boolean peerIsAgent;
    /** 有请求在途。重复发送防护就看它 */
    private boolean aiPending;
    /** 在途请求的句柄，页面销毁时取消 */
    private AiRequest aiRequest;
    /** 最近一条 AI 回复的 id：写库失败时用来判断「失败的是回复，不是我发的消息」 */
    private String lastAiMessageId;
    private TextView tvAiStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        if (conversationId == null) {
            finish();
            return;
        }

        Contact peer = repository.getPeer(conversationId);
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(peer == null ? getString(R.string.app_name) : peer.getName());
        // 只有 AGENT 类型的联系人才会触发真实模型调用（默认数据里的「小助手」就是）
        peerIsAgent = peer != null && peer.getType() == ContactType.AGENT;

        recyclerView = findViewById(R.id.rv_messages);
        etInput = findViewById(R.id.et_input);
        tvSend = findViewById(R.id.tv_send);
        ivPlus = findViewById(R.id.iv_plus);
        tvAiStatus = findViewById(R.id.tv_ai_status);

        // 不再用 stackFromEnd：它会把“不足一屏”的内容整体推到底部，
        // 新建会话一进去就看到消息贴底 + 上方一大块空白，不符合预期。
        // 改成默认从顶部排，有记录时由下面主动滚到最后一条。
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        this.layoutManager = layoutManager;

        adapter = new MessageAdapter(peer == null ? null : peer.getAvatar());
        adapter.setData(repository.getMessages(conversationId));
        recyclerView.setAdapter(adapter);

        findViewById(R.id.iv_back).setOnClickListener(this);
        findViewById(R.id.iv_more).setOnClickListener(this);
        findViewById(R.id.iv_voice).setOnClickListener(this);
        findViewById(R.id.iv_emoji).setOnClickListener(this);
        ivPlus.setOnClickListener(this);
        tvSend.setOnClickListener(this);

        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean hasText = s != null && s.toString().trim().length() > 0;
                // 有输入时显示「发送」，否则显示「+」，跟微信一致
                tvSend.setVisibility(hasText ? View.VISIBLE : View.GONE);
                ivPlus.setVisibility(hasText ? View.GONE : View.VISIBLE);
            }
        });

        etInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                    sendText();
                    return true;
                }
                return false;
            }
        });

        setupInsets();

        // 持续记录“当前是不是停在底部”，供键盘弹出/收起时判断要不要跟随
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                lastKnownAtBottom = isAtBottom();
            }
        });

        // 列表可用高度变化时（adjustResize 生效的情况），只有在底部才跟随，
        // 翻看历史记录时不会被强行拉到底部
        recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                      int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom && lastKnownAtBottom) {
                    v.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollToBottom();
                        }
                    });
                }
            }
        });

        scrollToBottom();
    }

    /**
     * 状态栏 / 导航栏 / 软键盘适配。
     *
     * 和另外几个页面共用 {@link InsetsHelper}，这里多出来的是键盘：
     * 上面那一套保证标题不会被状态栏压住，下面由 IME inset 负责把输入栏顶到键盘之上。
     * 三者互不重复 —— 系统已经处理的那部分 inset 是 0，我们只补没被管到的高度。
     *
     * 滚动跟随单独放在回调里做，是为了保留上一轮的结论：
     * 只有当列表本来停在最后一条时才跟着顶上去，翻看历史时不强行拉回底部。
     */
    private void setupInsets() {
        rootView = findViewById(R.id.chat_root);
        InsetsHelper.applyWithKeyboard(rootView, new InsetsHelper.Callback() {
            @Override
            public void onInsetsApplied(final int top, final int bottom) {
                // 回调时布局还没重排完，此刻读到的“是否在底部”正是变化前的状态
                if (bottom > 0 && isAtBottom()) {
                    rootView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollToBottom();
                        }
                    });
                }
            }
        });
    }

    /** 列表当前是否停在最后一条消息上 */
    private boolean isAtBottom() {
        if (adapter == null || layoutManager == null) {
            return true;
        }
        int lastVisible = layoutManager.findLastVisibleItemPosition();
        return lastVisible >= adapter.getItemCount() - 1;
    }

    /**
     * 页面回到前台（含第一次打开）：登记“本会话在前台”并标记已读。
     *
     * 放在 onResume 而不是 onCreate，是为了覆盖 Activity 被重建、
     * 或从后台切回来的情况；markConversationRead 是幂等的，重复调用不会误清零。
     */
    @Override
    protected void onResume() {
        super.onResume();
        repository.setForegroundConversation(conversationId);
        repository.markConversationRead(conversationId);
        // 接管写失败通道：发送消息就在本页，失败要立刻把气泡撤掉
        repository.setWriteFailureListener(this);
    }

    /**
     * 写库失败：数据层已经回滚了内存里的那条消息，这里按回滚后的数据重画，
     * 不让「发出去了」的假象留着 —— 磁盘上并没有这条消息。
     *
     * 两种情况必须分开说清楚：
     *  - 失败的是 AI 回复那条 → 请求成功了、回复没保存。用户的消息还在，只是没有回答。
     *  - 失败的是用户自己发的那条 → 消息本身没存下来。数据层已经弹了通用提示，这里不再重复。
     * 判据就是 messageId 和最近一条 AI 回复的 id 是否相同。
     */
    @Override
    public void onWriteFailed(String conversationId, String messageId) {
        if (adapter == null) {
            return;
        }
        adapter.setData(repository.getMessages(this.conversationId));
        scrollToBottom();
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (lastAiMessageId != null && lastAiMessageId.equals(messageId)) {
            Toast.makeText(this, R.string.msg_ai_save_failed, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 离开页面（返回列表、回桌面、跳到别的页）：注销前台会话。
     * 注销之后收到的消息才会计入未读。
     */
    @Override
    protected void onPause() {
        super.onPause();
        repository.setForegroundConversation(null);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_back) {
            finish();
        } else if (id == R.id.tv_send) {
            sendText();
        } else if (id == R.id.iv_more) {
            showMoreMenu();
        } else if (id == R.id.iv_voice) {
            Toast.makeText(this, R.string.msg_not_implemented, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.iv_emoji) {
            Toast.makeText(this, R.string.msg_not_implemented, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.iv_plus) {
            Toast.makeText(this, R.string.msg_not_implemented, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 唯一的发送入口。
     *
     * 顺序很重要：**先把用户消息存下来，再去要回复**。
     * 这样即便 AI 请求失败，用户说过的话也已经落库了，不会凭空消失；
     * 反过来（先请求后落库）则会出现「回复来了但原文没存」的错乱。
     */
    private void sendText() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        // 重复发送防护：上一条还在等回复时不发第二次。
        // 连续点两下会让同一段历史被并发送进模型，回报两条回复，历史也就乱了。
        if (peerIsAgent && aiPending) {
            Toast.makeText(this, R.string.ai_waiting, Toast.LENGTH_SHORT).show();
            return;
        }

        Message message = repository.sendTextMessage(conversationId, text);
        if (message == null) {
            // 会话不存在：不要凭空画一个气泡出去，直接提示
            Toast.makeText(this, R.string.msg_write_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        adapter.append(message);
        etInput.setText("");
        scrollToBottom();

        if (peerIsAgent) {
            requestAiReply();
        }
    }

    /**
     * 向 AI 要一条回复。
     *
     * 三个分支的界面含义刻意不同：
     *  - 拿到回复：追加气泡；
     *  - 请求失败：用户消息保留，只 Toast 失败原因（消息已经存了，不能撤）；
     *  - 回复保存失败：由 {@link #onWriteFailed} 处理，数据层已经把气泡回滚掉了。
     */
    private void requestAiReply() {
        aiPending = true;
        tvAiStatus.setVisibility(View.VISIBLE);
        tvAiStatus.setText(R.string.ai_thinking);

        aiRequest = AiReplyManager.getInstance().requestReply(conversationId,
                new AiReplyManager.Callback() {
                    @Override
                    public void onReply(Message message) {
                        aiPending = false;
                        aiRequest = null;
                        hideAiStatus();
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        lastAiMessageId = message.getId();
                        adapter.append(message);
                        scrollToBottom();
                    }

                    @Override
                    public void onError(AiError error) {
                        aiPending = false;
                        aiRequest = null;
                        hideAiStatus();
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        // 主动取消不是错误，不打扰用户
                        if (error.getCode() == AiError.Code.CANCELLED) {
                            return;
                        }
                        Log.w(TAG, "ai reply failed: " + AiError.describe(error));
                        Toast.makeText(ChatActivity.this, describeAiError(error),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void hideAiStatus() {
        if (tvAiStatus != null) {
            tvAiStatus.setVisibility(View.GONE);
        }
    }

    /** 把错误码翻成人能看懂的一句；具体原因（如状态码）一并带上 */
    private String describeAiError(AiError error) {
        switch (error.getCode()) {
            case NOT_CONFIGURED:
                return getString(R.string.ai_error_not_configured);
            case TIMEOUT:
                return getString(R.string.ai_error_timeout);
            case NETWORK:
                return getString(R.string.ai_error_network);
            case HTTP:
                return getString(R.string.ai_error_http, error.getHttpStatus());
            case PARSE:
                return getString(R.string.ai_error_parse);
            case EMPTY:
                return getString(R.string.ai_error_empty);
            case SAVE_FAILED:
                return getString(R.string.ai_error_save_failed);
            default:
                return getString(R.string.ai_error_unknown, error.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 页面没了就掐掉在途请求：回调随后到达时会被 isFinishing/isDestroyed 挡住，
        // 不会去更新一个已经销毁的界面
        if (aiRequest != null) {
            Log.d(TAG, "onDestroy: 取消在途 AI 请求");
            aiRequest.cancel();
            aiRequest = null;
        } else {
            Log.d(TAG, "onDestroy: 无在途 AI 请求");
        }
        aiPending = false;
    }

    private void scrollToBottom() {
        int count = adapter.getItemCount();
        if (count > 0) {
            recyclerView.scrollToPosition(count - 1);
        }
    }

    /**
     * 右上角「···」菜单。
     *
     * 里面两项是开发测试入口，不是微信的正式功能：单机没有真实来信，
     * 只能手动触发一条“对方消息”来验证未读逻辑。
     * 将来接 Agent 时，真实回复走 receiveTextMessage 的其它调用方，这两项可以直接删掉。
     */
    private void showMoreMenu() {
        final String[] items = {
                getString(R.string.debug_receive_here),
                getString(R.string.debug_receive_other)
        };
        final ListPopupWindow popup = new ListPopupWindow(this);
        popup.setAnchorView(findViewById(R.id.iv_more));
        popup.setWidth((int) (getResources().getDisplayMetrics().density * 200));
        popup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                Arrays.asList(items)));
        popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // 本会话正被打开：消息直接显示，不计未读
                    Message incoming = repository.receiveTextMessage(conversationId,
                            nextDebugText());
                    if (incoming != null) {
                        adapter.append(incoming);
                        scrollToBottom();
                    }
                } else {
                    // 别的会话来消息：当前不在那个会话里，未读应该 +1
                    String otherId = findOtherConversationId();
                    if (otherId != null) {
                        repository.receiveTextMessage(otherId, nextDebugText());
                    }
                }
                Toast.makeText(ChatActivity.this, R.string.debug_received, Toast.LENGTH_SHORT).show();
                popup.dismiss();
            }
        });
        popup.show();
    }

    /** 找一个是当前会话之外的会话，用于“别的会话来消息”的测试 */
    private String findOtherConversationId() {
        List<Conversation> all = repository.getConversations();
        for (Conversation c : all) {
            if (!c.getId().equals(conversationId)) {
                return c.getId();
            }
        }
        return null;
    }

    private String nextDebugText() {
        debugSeq++;
        return "（测试消息）" + debugSeq;
    }
}
