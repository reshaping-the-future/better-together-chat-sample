/*
 * Copyright (C) 2017 The Better Together Toolkit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package ac.robinson.bettertogether.plugin.chat;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

import ac.robinson.bettertogether.api.BasePluginActivity;
import ac.robinson.bettertogether.api.messaging.BroadcastMessage;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * This class demonstrates how to create a Better Together plugin, using a simple chat application as an example.
 * Messages
 * between devices are sent and received using {@link #sendMessage(BroadcastMessage)} and
 * {@link #onMessageReceived(BroadcastMessage)}. Plugin setup happens in AndroidManifest.xml - see that file for
 * interface and
 * other configuration.
 */
public class ChatActivity extends BasePluginActivity {

	private static final String SAVED_LIST = "saved_list";

	private RecyclerView mChatMessageView;
	private ChatMessageAdapter mChatMessageViewAdapter;

	private EditText mEnteredMessage;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setDisplayShowTitleEnabled(true);
		}

		ArrayList<MessageItem> messageItems;
		if (savedInstanceState != null) {
			messageItems = savedInstanceState.getParcelableArrayList(SAVED_LIST); // TODO: scroll to end on restore?
		} else {
			messageItems = new ArrayList<>();
		}

		mChatMessageViewAdapter = new ChatMessageAdapter(messageItems);
		mChatMessageView = findViewById(R.id.message_list);
		mChatMessageView.setLayoutManager(new LinearLayoutManager(ChatActivity.this, LinearLayoutManager.VERTICAL, false));
		mChatMessageView.setHasFixedSize(true);
		mChatMessageView.setAdapter(mChatMessageViewAdapter);

		mEnteredMessage = findViewById(R.id.entered_message);
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putParcelableArrayList(SAVED_LIST, mChatMessageViewAdapter.getItems());
		super.onSaveInstanceState(outState);
	}

	/**
	 * Messages from other devices arrive here to be processed by your plugin. Messages can be sent to other devices
	 * using
	 * {@link #sendMessage(BroadcastMessage)}.
	 *
	 * @param message The message received.
	 */
	@Override
	protected void onMessageReceived(@NonNull BroadcastMessage message) {
		mChatMessageViewAdapter.addMessage(new MessageItem(message.getMessage(), false));
	}

	public void handleClick(View view) {
		if (view.getId() == R.id.send_button) { // can't use resource id switch statements in library modules
			String messageText = mEnteredMessage.getText().toString();
			if (TextUtils.isEmpty(messageText)) {
				return;
			}
			mChatMessageViewAdapter.addMessage(new MessageItem(messageText, true));
			mEnteredMessage.setText("");

			BroadcastMessage message = new BroadcastMessage(BroadcastMessage.TYPE_DEFAULT, messageText);
			sendMessage(message);
		}
	}

	private static class MessageItem implements Parcelable {
		String mMessage;
		boolean mOwnMessage;

		MessageItem(String message, boolean ownMessage) {
			mMessage = message;
			mOwnMessage = ownMessage;
		}

		private MessageItem(Parcel in) {
			mMessage = in.readString();
			mOwnMessage = in.readByte() != 0;
		}

		public int describeContents() {
			return 0;
		}

		@NonNull
		@Override
		public String toString() {
			return mMessage + " (" + mOwnMessage + ")";
		}

		public void writeToParcel(Parcel out, int flags) {
			out.writeString(mMessage);
			out.writeByte((byte) (mOwnMessage ? 1 : 0));
		}

		public static final Creator<MessageItem> CREATOR = new Creator<MessageItem>() {
			public MessageItem createFromParcel(Parcel in) {
				return new MessageItem(in);
			}

			public MessageItem[] newArray(int size) {
				return new MessageItem[size];
			}
		};
	}

	private class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ChatMessageViewHolder> {
		private final ArrayList<MessageItem> mMessages;

		ChatMessageAdapter(ArrayList<MessageItem> initialItems) {
			mMessages = initialItems;
		}

		ArrayList<MessageItem> getItems() {
			return mMessages;
		}

		void addMessage(MessageItem message) {
			mMessages.add(message);
			notifyDataSetChanged();
			mChatMessageView.scrollToPosition(mMessages.size() - 1);
		}

		@NonNull
		@Override
		public ChatMessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			return new ChatMessageViewHolder(
					LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull ChatMessageViewHolder holder, int position) {
			Resources resources = getResources();
			MessageItem message = mMessages.get(position);
			if (message.mOwnMessage) {
				holder.mMessageRight.setText(message.mMessage);
				holder.mMessageRight.setBackgroundColor(resources.getColor(R.color.message_background_own));
				holder.mMessageRight.setTextColor(resources.getColor(R.color.message_text_own));
				holder.mMessageRight.setVisibility(View.VISIBLE);
				holder.mMessageLeft.setVisibility(View.GONE);
			} else {
				holder.mMessageLeft.setText(message.mMessage);
				holder.mMessageLeft.setBackgroundColor(resources.getColor(R.color.message_background_other));
				holder.mMessageLeft.setTextColor(resources.getColor(R.color.message_text_other));
				holder.mMessageLeft.setVisibility(View.VISIBLE);
				holder.mMessageRight.setVisibility(View.GONE);
			}
		}

		@Override
		public int getItemCount() {
			return mMessages.size();
		}

		class ChatMessageViewHolder extends RecyclerView.ViewHolder {
			final TextView mMessageLeft;
			final TextView mMessageRight;

			ChatMessageViewHolder(View itemView) {
				super(itemView);
				mMessageLeft = itemView.findViewById(R.id.message_text_left);
				mMessageRight = itemView.findViewById(R.id.message_text_right);
			}
		}
	}
}
