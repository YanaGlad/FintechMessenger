package com.example.emoji.fragments.message

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.emoji.App
import com.example.emoji.R
import com.example.emoji.api.model.Message
import com.example.emoji.databinding.FragmentMessageBinding
import com.example.emoji.fragments.delegateItem.DateDelegate
import com.example.emoji.fragments.delegateItem.MainAdapter
import com.example.emoji.fragments.delegateItem.MessageDelegate
import com.example.emoji.model.MessageModel
import com.example.emoji.model.Reaction
import com.example.emoji.support.MyCoolSnackbar
import com.example.emoji.support.toDelegateItemListWithDate
import com.example.emoji.viewState.elm.messanger.*
import kotlinx.serialization.ExperimentalSerializationApi
import vivid.money.elmslie.android.base.ElmFragment
import vivid.money.elmslie.core.ElmStoreCompat
import vivid.money.elmslie.core.store.Store
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


/**
 * @author y.gladkikh
 */
@ExperimentalSerializationApi
class MessageFragment : ElmFragment<MessageEvent, MessageEffect, MessengerState>() {

    private val args: MessageFragmentArgs by navArgs()

    private val streamName by lazy { args.stream.title }
    private val topicName by lazy { args.topic.title }
    private var reactionsList: List<Reaction> = emptyList()

    @Inject
    lateinit var messengerGlobalDI: MessengerGlobalDI

    private var cachedMessages: ArrayList<MessageModel> = arrayListOf()

    private lateinit var mainAdapter: MainAdapter

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!

    override val initEvent: MessageEvent = MessageEvent.UI.Init

    override fun createStore(): Store<MessageEvent, MessageEffect, MessengerState> {
        val actor = messengerGlobalDI.actor
        return ElmStoreCompat(
            initialState = MessengerState(streamName = streamName, topicName = topicName, isLoading = false),
            reducer = MessengerReducer(),
            actor = actor
        )
    }

    override fun render(state: MessengerState) {
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.recycleMessage.visibility = if (state.isLoading) View.GONE else View.VISIBLE

        initAdapter()
        //mainAdapter.submitList(cachedMessages.toDelegateItemListWithDate())
        setupMessageList(state)
        reactionsList = state.reactions.map {
            Reaction(
                userId = it.userId,
                emoji = it.emoji,
                emojiName = it.name,
                clicked = it.userId == state.myUserId
            )
        }
        mainAdapter.submitList(cachedMessages.toDelegateItemListWithDate())
    }

    override fun handleEffect(effect: MessageEffect) {
        when (effect) {
            is MessageEffect.HideKeyboard -> hideKeyboard()
            is MessageEffect.ShowEnexpectedError -> showErrorSnackbar("Unexpected error!")
            is MessageEffect.ShowNetworkError -> showErrorSnackbar("No internet connection!")
            is MessageEffect.UpdateMessageList -> mainAdapter.submitList(cachedMessages.toDelegateItemListWithDate())
        }
    }

    private fun setupMessageList(state: MessengerState) {
        val mappedList = (state.items as List<Message>)
            .map { it ->
                MessageModel(
                    id = it.id,
                    userId = it.authorId,
                    name = it.authorName,
                    picture = it.avatarUrl,
                    message = it.content,
                    date = convertDateFromUnixDay(it.time),
                    month = convertDateFromUnix(it.time).substring(0, 3),
                    isMe = state.myUserName == it.authorName,
                    listReactions = it.reactions.map {
                        Reaction(
                            userId = it.userId,
                            emoji = it.code,
                            emojiName = it.name,
                            clicked = state.myUserId == it.userId
                        )
                    })
            }

        mappedList.forEach {
            it.countedReactions = countEmoji(it)

            it.listReactions.forEach { reaction ->
                reaction.clicked = reaction.userId == 455747 //TODO REAL
            }
        }
        cachedMessages += mappedList

        mainAdapter.submitList(cachedMessages.toDelegateItemListWithDate())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity?.application as App).appComponent.inject(this)
        //   store.accept(MessageEvent.Internal.ReactionsLoading)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMessageBinding.inflate(layoutInflater)

        cachedMessages.clear()
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        initAdapter()
        setupSendPanel()
        setupSendingMessage()
    }

    private fun showErrorSnackbar(message: String) {
        store.accept(MessageEvent.Internal.StreamLoading(streamName, topicName))

        MyCoolSnackbar(
            layoutInflater,
            binding.root,
            message
        )
            .makeSnackBar()
            .show()

        binding.progressBar.visibility = View.GONE
        binding.recycleMessage.visibility = View.VISIBLE
    }

    private fun onSuccess() {
        cachedMessages.forEach {
            it.countedReactions = countEmoji(it)
        }

        mainAdapter.submitList(cachedMessages.toDelegateItemListWithDate())
        binding.progressBar.visibility = View.GONE
        binding.recycleMessage.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    private fun setupToolbar() {
        with(binding) {
            titleToolbar.text = "#${streamName}" //TODO args
            textToolbar.text = "#${topicName}"

            backArrowToolbar.setOnClickListener {
                findNavController().navigate(MessageFragmentDirections.actionMessageFragmentToChannelsFragment())
            }
        }
    }

    private fun setupSendPanel() {
        binding.sendPanel.apply {
            sendButton.also {
                it.isEnabled = false
                enterMessageEt.doOnTextChanged { _, _, _, _ ->
                    if (enterMessageEt.text.toString().isEmpty()) {
                        it.setImageDrawable(requireContext().getDrawable(R.drawable.delete))
                        it.isEnabled = false
                    } else {
                        it.setImageDrawable(requireContext().getDrawable(R.drawable.ic_delete))
                        it.isEnabled = true
                    }
                }
            }
        }
    }

    private fun setupSendingMessage() {
        binding.sendPanel.sendButton.setOnClickListener {
            hideKeyboard()

            store.accept(MessageEvent.Internal.MessageAdded(streamName, topicName, binding.sendPanel.enterMessageEt.text.toString(), IOException()))
            store.accept(MessageEvent.Internal.StreamLoading(streamName, topicName))

            binding.sendPanel.enterMessageEt.setText("")
        }
    }

    private fun convertDateFromUnix(date: Long): String {
        val sdf = SimpleDateFormat("MMMM d. yyyy. hh:mm", Locale.US)
        val dat = Date(date * 1000L)

        val finalString = sdf.format(dat)

        val calendar = GregorianCalendar()
        calendar.time = dat

        return finalString
    }

    private fun convertDateFromUnixDay(date: Long): String {
        val sdf = SimpleDateFormat("d", Locale.US)
        val dat = Date(date * 1000L)

        val finalString = sdf.format(dat)

        val calendar = GregorianCalendar()
        calendar.time = dat

        return finalString
    }

    private fun hideKeyboard() {
        val inputManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        inputManager.hideSoftInputFromWindow(
            binding.sendPanel.enterMessageEt.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    private fun showBottomSheetFragment(messageId: Int) {
        BottomSheetFragment(reactionsList) { reaction, _ ->
            updateElementWithReaction(messageId, reaction)
            mainAdapter.submitList(cachedMessages.toDelegateItemListWithDate())
        }.show(childFragmentManager, "bottom_tag")
    }

    private fun countEmoji(message: MessageModel): Map<String, Int> {
        val emojiCount = mutableMapOf<String, Int>()

        message.listReactions.forEach {
            val oldValue = emojiCount[it.emoji]
            if (oldValue == null) {
                emojiCount[it.emoji] = 1
            } else emojiCount[it.emoji] = oldValue + 1
        }
        return emojiCount
    }

    private fun updateElementWithReaction(messageId: Int, reaction: Reaction) {
        cachedMessages.indexOfFirst { it.id == messageId }.let {
            if (!reaction.clicked) {
                store.accept(MessageEvent.Internal.ReactionAdded(messageId, reaction.emojiName, IOException()))
            } else {
                store.accept(MessageEvent.Internal.ReactionRemoved(messageId, reaction.emojiName, topicName, streamName, IOException()))
            }

            store.accept(MessageEvent.Internal.StreamLoading(
                streamName = streamName,
                topicName = topicName,
                lastMessageId = 0,
                count = 1500
            ))
        }
    }

    fun findEmojiInList(emoji: String): Reaction? {
        for (reaction in reactionsList) {
            if (reaction.emoji == emoji) {
                return reaction
            }
        }
        return null
    }

    fun setClickedEmojiInList(emoji: String): Reaction? {
        for (reaction in reactionsList) {
            if (reaction.emoji == emoji) {
                 reaction.clicked = true
            }
        }
        return null
    }

    private fun initAdapter() {
        mainAdapter = MainAdapter()

        mainAdapter.apply {
            addDelegate(MessageDelegate(
                { item, _ -> showBottomSheetFragment(item.id) },
                { emoji, id ->
                    run {
                        //Toast.makeText(context, "Click ${emoji}", Toast.LENGTH_SHORT).show()
                        val reaction = findEmojiInList(emoji)
                        if (reaction != null) {
                            store.accept(MessageEvent.Internal.ReactionAdded(id, reaction.emojiName, IOException()))
                        }
                        setClickedEmojiInList(emoji)
                    }
                },
                { emoji, id ->
                    run {
                     //   Toast.makeText(context, "Click REMOVE ${emoji}", Toast.LENGTH_SHORT).show()
                        val reaction = findEmojiInList(emoji)
                        if (reaction != null) {
                            store.accept(MessageEvent.Internal.ReactionRemoved(id, reaction.emojiName, topicName, streamName, IOException()))
                        }
                    }
                },
            )
            )
            addDelegate(DateDelegate())
        }

        binding.recycleMessage.adapter = mainAdapter
    }
}
