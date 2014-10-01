package com.example.pl2303hx;

import java.util.HashMap;
import java.util.Iterator;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity{
	
	private static final String ACTION_USB_PERMISSION = "com.example.pl2303hx.USB_PERMISSION";
	private Button bt1,bt2,bt3;
	private EditText ed1;
	private TextView tv1;
	private UsbManager mUsbManager;
	private Data_SR mData_SR;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		object();
	}
	
	private void object(){
		bt1 = (Button)this.findViewById(R.id.bt1);
		bt2 = (Button)this.findViewById(R.id.bt2);
		bt3 = (Button)this.findViewById(R.id.bt3);
		ed1 = (EditText)this.findViewById(R.id.ed1);
		tv1 = (TextView)this.findViewById(R.id.tv1);
		bt1.setOnClickListener(click_);
		bt2.setOnClickListener(click_);
		bt3.setOnClickListener(click_);
		bt3.setEnabled(false);
		bt2.setEnabled(false);
	}
	
	private void USB_object(){
		mUsbManager = (UsbManager)this.getSystemService(Context.USB_SERVICE);
		this.registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_USB_PERMISSION));
		getdevicelist();
	}
	
	private void getdevicelist(){
		PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		HashMap<String,UsbDevice> mHashMap = mUsbManager.getDeviceList();
		Iterator<UsbDevice> mIterator = mHashMap.values().iterator();
		while(mIterator.hasNext()){
			UsbDevice mUsbDevice = mIterator.next();
			mUsbManager.requestPermission(mUsbDevice,mPendingIntent);
		}
	}
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if(ACTION_USB_PERMISSION.equals(action)){
				synchronized(this){
					UsbDevice mUsbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)&&(mUsbDevice!=null)){
						//permission granted
						mData_SR = new Data_SR(mUsbManager,mUsbDevice,MainActivity.this,tv1);
						boolean init_ = mData_SR.initialize();
						if(init_==true){
							bt2.setEnabled(true);
							bt3.setEnabled(true);
							mData_SR.latch = true;
							mData_SR.data_read();
							Toast.makeText(getApplicationContext(), "initialize is successed", Toast.LENGTH_SHORT).show();
						}else{
							Toast.makeText(getApplicationContext(), "initialize is stuck",Toast.LENGTH_SHORT).show();
						}
					}
				}
			}
		}
		
	};
	
	
	View.OnClickListener click_ = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			int id = v.getId();
			switch(id){
			case R.id.bt1:
				USB_object();
				break;
			case R.id.bt2:
				if(ed1.getText().length()>0){
					boolean send_ = mData_SR.send(ed1.getText().toString());
					if(send_==true)Toast.makeText(getApplicationContext(), "send successed", Toast.LENGTH_SHORT).show();
				}
				break;
			case R.id.bt3:
				mData_SR.close();
				finish();
				break;
			}
		}
	};

	
	
}
