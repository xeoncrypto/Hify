package com.amsavarthan.hify.adapters;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amsavarthan.hify.R;
import com.amsavarthan.hify.models.Post;
import com.amsavarthan.hify.ui.activities.friends.FriendProfile;
import com.amsavarthan.hify.ui.activities.post.CommentsActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.github.ivbaranov.mfb.MaterialFavoriteButton;
import com.github.marlonlom.utilities.timeago.TimeAgo;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import me.grantland.widget.AutofitTextView;

/**
 * Created by amsavarthan on 22/2/18.
 */

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {

    private List<Post> postList;
    private Context context;
    private FirebaseFirestore mFirestore;
    private FirebaseUser mCurrentUser;
    private boolean isOwner;

    public PostsAdapter(List<Post> postList, Context context) {
        this.postList = postList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed_post, parent, false);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mCurrentUser = mAuth.getCurrentUser();

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        if(postList.get(position).getUserId().equals(mCurrentUser.getUid())){
            isOwner=true;
            holder.delete.setVisibility(View.VISIBLE);

            holder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   new MaterialDialog.Builder(context)
                           .title("Delete post")
                           .content("Are you sure do you want to delete this post?")
                           .positiveText("Yes")
                           .negativeText("No")
                           .onPositive(new MaterialDialog.SingleButtonCallback() {
                               @Override
                               public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                                   final ProgressDialog pdialog=new ProgressDialog(context);
                                   pdialog.setMessage("Please wait...");
                                   pdialog.setIndeterminate(true);
                                   pdialog.setCancelable(false);
                                   pdialog.setCanceledOnTouchOutside(false);
                                   pdialog.show();

                                   dialog.dismiss();
                                   FirebaseFirestore.getInstance().collection("Posts")
                                           .document(postList.get(holder.getAdapterPosition()).postId)
                                           .delete()
                                           .addOnSuccessListener(new OnSuccessListener<Void>() {
                                               @Override
                                               public void onSuccess(Void aVoid) {
                                                   pdialog.dismiss();
                                                   notifyItemRemoved(holder.getAdapterPosition());
                                                   notifyDataSetChanged();
                                                   Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show();
                                               }
                                           })
                                           .addOnFailureListener(new OnFailureListener() {
                                               @Override
                                               public void onFailure(@NonNull Exception e) {
                                                   pdialog.dismiss();
                                                   Log.e("error",e.getLocalizedMessage());
                                               }
                                           });

                               }
                           })
                           .onNegative(new MaterialDialog.SingleButtonCallback() {
                               @Override
                               public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                   dialog.dismiss();
                               }
                           }).show();
                }
            });

        }else{
            isOwner=false;
            holder.delete.setVisibility(View.GONE);
        }

        try {
            setupViews(holder);
        }catch (Exception e){
            e.printStackTrace();
        }


        try {
            mFirestore.collection("Users")
                    .document(postList.get(position).getUserId())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(final DocumentSnapshot documentSnapshot) {

                            try {
                                if (!documentSnapshot.getString("username").equals(postList.get(holder.getAdapterPosition()).getUsername()) &&
                                        !documentSnapshot.getString("image").equals(postList.get(holder.getAdapterPosition()).getUserimage())) {

                                    Map<String, Object> postMap = new HashMap<>();
                                    postMap.put("username", documentSnapshot.getString("username"));
                                    postMap.put("userimage", documentSnapshot.getString("image"));

                                    mFirestore.collection("Posts")
                                            .document(postList.get(holder.getAdapterPosition()).postId)
                                            .update(postMap)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.i("post_update", "success");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.i("post_update", "failure");
                                                }
                                            });

                                    holder.user_name.setText(documentSnapshot.getString("username"));

                                    Glide.with(context)
                                            .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_g_2))
                                            .load(documentSnapshot.getString("image"))
                                            .into(holder.user_image);


                                } else if (!documentSnapshot.getString("username").equals(postList.get(holder.getAdapterPosition()).getUsername())) {


                                    Map<String, Object> postMap = new HashMap<>();
                                    postMap.put("username", documentSnapshot.getString("username"));

                                    mFirestore.collection("Posts")
                                            .document(postList.get(holder.getAdapterPosition()).postId)
                                            .update(postMap)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.i("post_update", "success");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.i("post_update", "failure");
                                                }
                                            });

                                    holder.user_name.setText(documentSnapshot.getString("username"));

                                } else if (!documentSnapshot.getString("image").equals(postList.get(holder.getAdapterPosition()).getUserimage())) {

                                    Map<String, Object> postMap = new HashMap<>();
                                    postMap.put("userimage", documentSnapshot.getString("image"));

                                    mFirestore.collection("Posts")
                                            .document(postList.get(holder.getAdapterPosition()).postId)
                                            .update(postMap)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.i("post_update", "success");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.i("post_update", "failure");
                                                }
                                            });

                                    Glide.with(context)
                                            .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_g_2))
                                            .load(documentSnapshot.getString("image"))
                                            .into(holder.user_image);

                                }


                            }catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("Error", e.getMessage());
                        }
                    });
        }catch (Exception ex){
            Log.w("error","fastscrolled",ex);
        }

        holder.user_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FriendProfile.startActivity(context,postList.get(holder.getAdapterPosition()).getUserId());
            }
        });

        holder.comment_btn.setOnFavoriteAnimationEndListener(new MaterialFavoriteButton.OnFavoriteAnimationEndListener() {
            @Override
            public void onAnimationEnd(MaterialFavoriteButton buttonView, boolean favorite) {


                    String desc = "<b>" + postList.get(holder.getAdapterPosition()).getUsername() + "</b> " + postList.get(holder.getAdapterPosition()).getDescription();
                    CommentsActivity.startActivity(context, postList,desc, holder.getAdapterPosition(),isOwner);


            }
        });

        holder.share_btn.setOnFavoriteAnimationEndListener(new MaterialFavoriteButton.OnFavoriteAnimationEndListener() {
            @Override
            public void onAnimationEnd(MaterialFavoriteButton buttonView, boolean favorite) {
                if (postList.get(holder.getAdapterPosition()).getImage().equals("no_image")) {

                    Intent intent = new Intent(Intent.ACTION_SEND)
                            .setType("image/*");
                    //ByteArrayOutputStream stream=new ByteArrayOutputStream();
                    intent.putExtra(Intent.EXTRA_STREAM, getBitmapUri(getBitmap(holder.mImageholder), holder, "hify_user_" + postList.get(holder.getAdapterPosition()).getUserId()));
                    try {
                        context.startActivity(Intent.createChooser(intent, "Share using..."));
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                    }

                } else {

                    Intent intent = new Intent(Intent.ACTION_SEND)
                            .setType("image/*");
                    intent.putExtra(Intent.EXTRA_STREAM, getBitmapUri(getBitmap(holder.post_image), holder, "hify_user_" + postList.get(holder.getAdapterPosition()).getUserId()));
                    try {
                        context.startActivity(Intent.createChooser(intent, "Share using..."));
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }

        });


    }

    private Uri getBitmapUri(Bitmap bitmap, ViewHolder holder, String name) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);

        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, postList.get(holder.getAdapterPosition()).postId, "Post by " + name);
        return Uri.parse(path);
    }

    private Bitmap getBitmap(FrameLayout view) {

        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            bgDrawable.draw(canvas);
        } else {
            canvas.drawColor(Color.parseColor("#212121"));
        }
        view.draw(canvas);

        return bitmap;
    }

    private Bitmap getBitmap(ImageView view) {

        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            bgDrawable.draw(canvas);
        } else {
            canvas.drawColor(Color.parseColor("#212121"));
        }
        view.draw(canvas);

        return bitmap;
    }

    private void setupViews(ViewHolder holder) {

        int pos = holder.getAdapterPosition();

        getLikeandFav(holder);
        getCounts(holder);

        holder.user_name.setText(postList.get(pos).getUsername());

        Glide.with(context)
                .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_g_2))
                .load(postList.get(pos).getUserimage())
                .into(holder.user_image);

        String timeAgo = TimeAgo.using(Long.parseLong(postList.get(pos).getTimestamp()));

        holder.timestamp.setText(timeAgo);

        if (postList.get(pos).getImage().equals("no_image")) {

            holder.post_image.setVisibility(View.GONE);
            holder.post_desc.setVisibility(View.GONE);
            setmImageHolderBg(postList.get(pos).getColor(), holder.mImageholder);
            holder.post_text.setVisibility(View.VISIBLE);
            holder.post_text.setText(postList.get(pos).getDescription());

        } else {

            holder.post_text.setVisibility(View.GONE);
            holder.post_image.setVisibility(View.VISIBLE);
            holder.post_desc.setVisibility(View.VISIBLE);
            String desc = "<b>" + postList.get(pos).getUsername() + "</b> " + postList.get(pos).getDescription();
            holder.post_desc.setText(Html.fromHtml(desc));

            Glide.with(context)
                    .load(postList.get(pos).getImage())
                    .into(holder.post_image);

        }
    }

    private void getLikeandFav(final ViewHolder holder) {

        //forLiked
        mFirestore.collection("Posts")
                .document(postList.get(holder.getAdapterPosition()).postId)
                .collection("Liked_Users")
                .document(mCurrentUser.getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        if (documentSnapshot.exists()) {
                            boolean liked = documentSnapshot.getBoolean("liked");

                            if (liked) {
                                holder.like_btn.setFavorite(true,false);
                            } else {
                                holder.like_btn.setFavorite(false,false);
                            }
                        } else {
                            Log.e("Like", "No document found");

                        }

                        holder.like_btn.setOnFavoriteChangeListener(new MaterialFavoriteButton.OnFavoriteChangeListener() {
                            @Override
                            public void onFavoriteChanged(MaterialFavoriteButton buttonView, boolean favorite) {
                                if(favorite) {
                                    Map<String, Object> likeMap = new HashMap<>();
                                    likeMap.put("liked", true);

                                    try {

                                        mFirestore.collection("Posts")
                                                .document(postList.get(holder.getAdapterPosition()).postId)
                                                .collection("Liked_Users")
                                                .document(mCurrentUser.getUid())
                                                .set(likeMap)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        holder.like_count.setText(String.valueOf(Integer.parseInt(holder.like_count.getText().toString())+1));
                                                        //Toast.makeText(context, "Liked post '" + postList.get(holder.getAdapterPosition()).postId, Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.e("Error like", e.getMessage());
                                                    }
                                                });
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }else{
                                    Map<String, Object> likeMap = new HashMap<>();
                                    likeMap.put("liked", false);

                                    try {

                                        mFirestore.collection("Posts")
                                                .document(postList.get(holder.getAdapterPosition()).postId)
                                                .collection("Liked_Users")
                                                .document(mCurrentUser.getUid())
                                                .set(likeMap)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        holder.like_count.setText(String.valueOf(Integer.parseInt(holder.like_count.getText().toString())-1));
                                                        //Toast.makeText(context, "Unliked post '" + postList.get(holder.getAdapterPosition()).postId, Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.e("Error unlike", e.getMessage());
                                                    }
                                                });
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });



                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Error Like", e.getMessage());
                    }
                });

        //forFavourite
        mFirestore.collection("Posts")
                .document(postList.get(holder.getAdapterPosition()).postId)
                .collection("Saved_Users")
                .document(mCurrentUser.getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        if (documentSnapshot.exists()) {
                            boolean fav = documentSnapshot.getBoolean("Saved");

                            if (fav) {
                                holder.sav_button.setFavorite(true,false);
                            } else {
                                holder.sav_button.setFavorite(false,false);
                            }
                        } else {
                            Log.e("Fav", "No document found");

                        }

                        holder.sav_button.setOnFavoriteChangeListener(new MaterialFavoriteButton.OnFavoriteChangeListener() {
                            @Override
                            public void onFavoriteChanged(MaterialFavoriteButton buttonView, boolean favorite) {
                                if(favorite) {

                                    Map<String, Object> favMap = new HashMap<>();
                                    favMap.put("Saved", true);

                                    try {

                                        mFirestore.collection("Posts")
                                                .document(postList.get(holder.getAdapterPosition()).postId)
                                                .collection("Saved_Users")
                                                .document(mCurrentUser.getUid())
                                                .set(favMap)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {

                                                        Map<String, Object> postMap = new HashMap<>();
                                                        postMap.put("userId", postList.get(holder.getAdapterPosition()).getUserId());
                                                        postMap.put("name", postList.get(holder.getAdapterPosition()).getName());
                                                        postMap.put("username", postList.get(holder.getAdapterPosition()).getUsername());
                                                        postMap.put("timestamp", postList.get(holder.getAdapterPosition()).getTimestamp());
                                                        postMap.put("image", postList.get(holder.getAdapterPosition()).getImage());
                                                        postMap.put("description", postList.get(holder.getAdapterPosition()).getDescription());
                                                        postMap.put("color", postList.get(holder.getAdapterPosition()).getColor());

                                                        mFirestore.collection("Users")
                                                                .document(mCurrentUser.getUid())
                                                                .collection("Saved_Posts")
                                                                .document(postList.get(holder.getAdapterPosition()).postId)
                                                                .set(postMap)
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        // Toast.makeText(context, "Added to Saved_Posts, post '" + postList.get(holder.getAdapterPosition()).postId, Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }).addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Log.e("Error add fav", e.getMessage());
                                                            }
                                                        });
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.e("Error fav", e.getMessage());
                                                    }
                                                });
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }

                                }else {

                                    Map<String, Object> favMap = new HashMap<>();
                                    favMap.put("Saved", false);

                                    try {

                                        mFirestore.collection("Posts")
                                                .document(postList.get(holder.getAdapterPosition()).postId)
                                                .collection("Saved_Users")
                                                .document(mCurrentUser.getUid())
                                                .set(favMap)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {

                                                        mFirestore.collection("Users")
                                                                .document(mCurrentUser.getUid())
                                                                .collection("Saved_Posts")
                                                                .document(postList.get(holder.getAdapterPosition()).postId)
                                                                .delete()
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        // Toast.makeText(context, "Removed from Saved_Posts, post '" + postList.get(holder.getAdapterPosition()).postId, Toast.LENGTH_SHORT).show();
                                                                    }
                                                                })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        Log.e("Error remove fav", e.getMessage());
                                                                    }
                                                                });

                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.e("Error fav", e.getMessage());
                                                    }
                                                });

                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Error Fav", e.getMessage());
                    }
                });

    }

    private void setmImageHolderBg(String color, FrameLayout mImageholder) {
        switch (Integer.parseInt(color)) {
            case 1:
                mImageholder.setBackgroundResource(R.drawable.gradient_9);
                break;
            case 2:
                mImageholder.setBackgroundResource(R.drawable.gradient_7);
                break;
            case 3:
                mImageholder.setBackgroundResource(R.drawable.gradient_8);
                break;
            case 4:
                mImageholder.setBackgroundResource(R.drawable.gradient_4);
                break;
            case 5:
                mImageholder.setBackgroundResource(R.drawable.gradient_1);
                break;
            case 6:
                mImageholder.setBackgroundResource(R.drawable.gradient_3);
                break;
            case 7:
                mImageholder.setBackgroundResource(R.drawable.gradient_2);
                break;
            case 8:
                mImageholder.setBackgroundResource(R.drawable.gradient_11);
                break;
        }

    }

    private void getCounts(final ViewHolder holder) {

        mFirestore.collection("Posts")
                .document(postList.get(holder.getAdapterPosition()).postId)
                .collection("Liked_Users")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(final QuerySnapshot querySnapshot) {
                        holder.like_count.setText(String.valueOf(querySnapshot.size()));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Error",e.getMessage());
                    }
                });

        /*mFirestore.collection("Posts")
                .document(postList.get(holder.getAdapterPosition()).postId)
                .collection("Saved_Users")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        fav.setText(String.valueOf(querySnapshot.size()));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Error",e.getMessage());
                    }
                });*/


    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private CircleImageView user_image;
        private TextView user_name, timestamp, post_desc;
        private ImageView post_image;
        private MaterialFavoriteButton sav_button, like_btn, share_btn, comment_btn;
        private FrameLayout mImageholder;
        private AutofitTextView post_text;
        private TextView like_count;
        private ImageView delete;

        public ViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            user_image = mView.findViewById(R.id.post_user_image);
            like_count = mView.findViewById(R.id.like_count);
            user_name = mView.findViewById(R.id.post_username);
            timestamp = mView.findViewById(R.id.post_timestamp);
            post_desc = mView.findViewById(R.id.post_desc);
            post_image = mView.findViewById(R.id.post_image);
            post_text = mView.findViewById(R.id.post_text);
            like_btn = mView.findViewById(R.id.like_button);
            comment_btn = mView.findViewById(R.id.comment_button);
            share_btn = mView.findViewById(R.id.share_button);
            delete = mView.findViewById(R.id.delete_button);
            sav_button = mView.findViewById(R.id.save_button);
            mImageholder = mView.findViewById(R.id.image_holder);

        }
    }

}