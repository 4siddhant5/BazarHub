package com.siddhant.bazarhub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    public interface OnUserActionListener {
        void onBlockToggle(UserModel user);
        void onDelete(UserModel user);
    }

    private List<UserModel> users;
    private OnUserActionListener listener;

    public UsersAdapter(List<UserModel> users, OnUserActionListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = users.get(position);

        holder.tvName.setText(user.getName());
        holder.tvEmail.setText(user.getEmail());
        holder.tvRole.setText(user.getRole());

        // Update Block/Unblock button text
        if (user.isBlocked()) {
            holder.btnBlock.setText("Unblock");
            holder.btnBlock.setBackgroundTintList(holder.itemView.getContext()
                    .getResources().getColorStateList(android.R.color.holo_green_dark));
        } else {
            holder.btnBlock.setText("Block");
            holder.btnBlock.setBackgroundTintList(holder.itemView.getContext()
                    .getResources().getColorStateList(android.R.color.holo_red_light));
        }

        // Block/Unblock Action
        holder.btnBlock.setOnClickListener(v -> {
            user.setBlocked(!user.isBlocked());

            // Update button text immediately
            if (user.isBlocked()) {
                holder.btnBlock.setText("Unblock");
                holder.btnBlock.setBackgroundTintList(holder.itemView.getContext()
                        .getResources().getColorStateList(android.R.color.holo_green_dark));
            } else {
                holder.btnBlock.setText("Block");
                holder.btnBlock.setBackgroundTintList(holder.itemView.getContext()
                        .getResources().getColorStateList(android.R.color.holo_red_light));
            }

            // Notify listener for Firestore update
            listener.onBlockToggle(user);
        });

        // Delete Action
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole;
        Button btnBlock, btnDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            tvRole = itemView.findViewById(R.id.tvUserRole);
            btnBlock = itemView.findViewById(R.id.btnBlockUser);
            btnDelete = itemView.findViewById(R.id.btnDeleteUser);
        }
    }
}
