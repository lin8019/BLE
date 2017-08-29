package tw.com.fulldot.bluetoothlowenergy;


import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

public class BLE_ViewHolder extends RecyclerView.ViewHolder{
    public TextView textView;

    public BLE_ViewHolder(View itemView) {
        super(itemView);
        textView = itemView.findViewById(R.id.myTextView);
    }
}
