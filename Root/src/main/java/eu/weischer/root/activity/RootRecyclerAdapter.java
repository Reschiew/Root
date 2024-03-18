package eu.weischer.root.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Method;
import java.util.ArrayList;

import eu.weischer.root.application.Logger;
@SuppressWarnings("unused")
public class RootRecyclerAdapter<B extends androidx.databinding.ViewDataBinding,T>
    extends RecyclerView.Adapter<RootRecyclerAdapter<B,T>.ViewHolder>{
    public class ViewHolder extends RecyclerView.ViewHolder {
        private final B binding;
        public ViewHolder(@NonNull B binding, GestureConstraintLayout.OnGesture onGesture) {
            super(binding.getRoot());
            this.binding = binding;
            if (onGesture != null)
                ((GestureConstraintLayout)binding.getRoot()).setOnGesture(onGesture);
        }
    }
    private final static Logger.LogAdapter log = Logger.getLogAdapter("RootRecyclerAdapter");
    private LayoutInflater layoutInflater;
    private GestureConstraintLayout.OnGesture onGesture;
    private ArrayList<T> list;
    private int layoutId;
    private Method setModelMethod = null;

    @SuppressWarnings("unused")
    public RootRecyclerAdapter(Context context, ArrayList<T> list, int layoutId, GestureConstraintLayout.OnGesture onGesture ) {
        try {
            this.layoutInflater = LayoutInflater.from(context);
            this.onGesture = onGesture;
            this.list = list;
            this.layoutId = layoutId;
        } catch (Exception ex) {
            log.e(ex, "Error during initialization");
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    @SuppressWarnings("unused")
    public void updateList(ArrayList<T> list) {
        this.list = list;
        notifyDataSetChanged();
    }
    @SuppressWarnings("unused")
    public void setList(ArrayList<T> list) {
        this.list = list;
    }
    @SuppressWarnings("unused")
    public ArrayList<T> getList() {
        return list;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            B binding = DataBindingUtil.inflate(layoutInflater, layoutId, parent, false);
            return new ViewHolder(binding, onGesture);
        } catch (Exception ex) {
            log.e(ex, "Error during onCreateViewHolder");
            B binding = DataBindingUtil.inflate(layoutInflater, layoutId, parent, false);
            return new ViewHolder(binding, onGesture);
        }
    }
    @Override
    public int getItemCount() {
        return list.size();
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            if (setModelMethod == null) {
                Method[] methods = holder.binding.getClass().getMethods();
                for (Method method : methods)
                    if (method.getName().equals("setModel"))
                        setModelMethod = method;
            }
            T model = list.get(position);
//            Method method = holder.binding.getClass().getMethod("setModel", model.getClass());
            setModelMethod.invoke(holder.binding, model);
        } catch (Exception ex) {
            log.e(ex,"Exception during onBindViewHolder");
        }
    }
}
