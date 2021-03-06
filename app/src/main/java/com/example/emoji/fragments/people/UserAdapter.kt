package com.example.emoji.fragments.people

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.emoji.databinding.UserItemBinding
import com.example.emoji.model.UserModel
import com.example.emoji.support.loadImage

/**
 * @author y.gladkikh
 */
class UserAdapter(
    private val onUserClick: OnUserClickListener,
) : ListAdapter<UserModel, UserAdapter.ViewHolder>(DiffCallback()) {

    interface OnUserClickListener {
        fun onUserClick(userModel: UserModel, position: Int)
    }

    class DiffCallback : DiffUtil.ItemCallback<UserModel>() {
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem == newItem
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            UserItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onUserClick
        )
    }

    class ViewHolder(
        private val binding: UserItemBinding,
        private val onUserClick: OnUserClickListener,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UserModel) {
            loadImage(itemView.context, item.picture, binding.avatar)
            binding.name.text = item.name
            binding.email.text = item.email

            itemView.setOnClickListener {
                onUserClick.onUserClick(item, adapterPosition)
            }
        }
    }
}
