package com.example.chen.simplecolorbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;


//import com.j256.simplemagic.ContentInfo;
//import com.j256.simplemagic.ContentInfoUtil;

import java.io.File;
import java.net.URLConnection;
import java.util.UUID;

public class MainActivity extends Activity {
    protected static final String TAG = "Main";

    solvePicture solve;

    private static Context mContext;


    public static final int MESSAGE_UI_DEBUG_VIEW=1;
    public static final int MESSAGE_UI_INFO_VIEW=2;

    public static final int REQUEST_CODE_FILE_PATH_INPUT=1;
    public static final int REQUEST_CODE_FILE_PATH_TRUTH=2;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    final Handler mHandler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            String text;
            switch (msg.what){
                case MESSAGE_UI_DEBUG_VIEW:
                    text=(String)msg.obj;
                    TextView debugView=(TextView)findViewById(R.id.debug_view);
                    debugView.setText(text);
                    return true;
                case MESSAGE_UI_INFO_VIEW:
                    text=(String)msg.obj;
                    TextView infoView=(TextView)findViewById(R.id.info_view);
                    infoView.setText(text);
                    return true;
                default:
                    return false;
            }
        }
    });

    /*
    
     */
    /**
     * 界面初始化,设置界面,调用CameraSettings()设置相机参数
     *
     * @param savedInstanceState 默认参数
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    CAMERA_REQUEST_CODE);
        }*/
        mContext=this;

        TextView debugView = (TextView) findViewById(R.id.debug_view);
        TextView infoView = (TextView) findViewById(R.id.info_view);
        debugView.setGravity(Gravity.BOTTOM);
        infoView.setGravity(Gravity.BOTTOM);


        Button buttonFilePathInput=(Button)findViewById(R.id.button_file_path_input);
        buttonFilePathInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFilePath(REQUEST_CODE_FILE_PATH_INPUT);
            }
        });
        Button buttonFilePathTruth=(Button)findViewById(R.id.button_file_path_truth);
        buttonFilePathTruth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFilePath(REQUEST_CODE_FILE_PATH_TRUTH);
            }
        });



        sharedPref=getSharedPreferences("main", Context.MODE_PRIVATE);
        editor=sharedPref.edit();

        ToggleButton toggleButtonFileNameCreated=(ToggleButton)findViewById(R.id.toggle_file_name_created);
        toggleButtonFileNameCreated.setTag("AUTO_GENERATE_FILE_NAME_CREATED");
        toggleButtonFileNameCreated.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                EditText editTextFileNameCreated=(EditText)findViewById(R.id.file_name_created);
                if(isChecked){
                    String randomFileName= UUID.randomUUID().toString();
                    editTextFileNameCreated.setText(randomFileName+".txt");
                    editTextFileNameCreated.setEnabled(false);
                }else{
                    editTextFileNameCreated.setEnabled(true);
                }
                editor.putBoolean((String)buttonView.getTag(),isChecked);
                editor.apply();
            }
        });



        final EditText editTextFileNameCreated=(EditText)findViewById(R.id.file_name_created);
        editTextFileNameCreated.setTag("FILE_NAME_CREATED");
        editTextFileNameCreated.setText(sharedPref.getString((String)editTextFileNameCreated.getTag(),""));
        editTextFileNameCreated.addTextChangedListener(new EditTextTextWatcher(editTextFileNameCreated));

        final EditText editTextFilePathInput=(EditText)findViewById(R.id.file_path_input);
        editTextFilePathInput.setTag("FILE_PATH_INPUT");
        editTextFilePathInput.setText(sharedPref.getString((String)editTextFilePathInput.getTag(),""));
        editTextFilePathInput.addTextChangedListener(new EditTextTextWatcher(editTextFilePathInput));

        final EditText editTextFilePathTruth=(EditText)findViewById(R.id.file_path_truth);
        editTextFilePathTruth.setTag("FILE_PATH_TRUTH");
        editTextFilePathTruth.setText(sharedPref.getString((String)editTextFilePathTruth.getTag(),""));
        editTextFilePathTruth.addTextChangedListener(new EditTextTextWatcher(editTextFilePathTruth));

        toggleButtonFileNameCreated.setChecked(sharedPref.getBoolean((String)toggleButtonFileNameCreated.getTag(),false));
    }
    /*public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode){
            case WRITE_EXTERNAL_STORAGE_REQUEST_CODE:{
                if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        &&grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    int a = 0;
                }
                else{
                    //用户不同意，自行处理即可
                    finish();
                }
                return;
            }
            case CAMERA_REQUEST_CODE:{
                if (permissions[0].equals(Manifest.permission.CAMERA)
                        &&grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    int a = 0;
                }
                else{
                    //用户不同意，自行处理即可
                    finish();
                }
                return;
            }
        }

    }*/



    public static Context getContext(){
        return mContext;
    }

    private class EditTextTextWatcher implements TextWatcher{
        private EditText mEditText;

        public EditTextTextWatcher(EditText editText){
            mEditText=editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            editor.putString((String)mEditText.getTag(),mEditText.getText().toString());
            editor.apply();
        }
    }

    public void saveFrames(View view){
        SaveFramesFragment fragment=new SaveFramesFragment();
        getFragmentManager().beginTransaction().replace(R.id.left_part, fragment).addToBackStack(null).commit();
        getFragmentManager().executePendingTransactions();
    }
    private void getFilePath(int requestCode){
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if(intent.resolveActivity(getPackageManager())!=null){
            startActivityForResult(Intent.createChooser(intent, "Select a File"), requestCode);
        }else{
            new AlertDialog.Builder(this).setTitle("未找到文件管理器")
                    .setMessage("请安装文件管理器以选择文件")
                    .setPositiveButton("确定",null)
                    .show();
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        int id=0;
        String path = "";
        switch (requestCode){
            case REQUEST_CODE_FILE_PATH_INPUT:
                id = R.id.file_path_input;

                if( data != null)
                    path =  data.getData().getPath();//这里是转换媒体的内部路径和绝对路径
                /*if(path.contains("external")){//华为平板处理
                    String[] proj = {MediaStore.Images.Media.DATA};
                    //好像是Android多媒体数据库的封装接口，具体的看Android文档
                    Cursor cursor = managedQuery(data.getData(), proj, null, null, null);
                    //按我个人理解 这个是获得用户选择的图片的索引值
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    //将光标移至开头 ，这个很重要，不小心很容易引起越界
                    cursor.moveToFirst();
                    //最后根据索引值获取图片路径
                    path = cursor.getString(column_index);
                }
                else*/ if(path.contains(":")){//三星手机处理
                    path =  Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + path.split(":")[1];
                }
                break;
            case REQUEST_CODE_FILE_PATH_TRUTH:
                id=R.id.file_path_truth;
                path = data.getData().getPath();
                break;
        }
        if (resultCode == RESULT_OK) {
            EditText editText = (EditText) findViewById(id);
            editText.setText(path);
        }
    }

    /**
     * 处理视频文件,从视频帧识别二维码
     *
     * @param view 默认参数
     */
    public void processVideo(View view) {
        EditText editTextVideoFilePath = (EditText) findViewById(R.id.file_path_input);
        final String videoFilePath = editTextVideoFilePath.getText().toString();
        EditText editTextFileName = (EditText) findViewById(R.id.file_name_created);
        final String newFileName = editTextFileName.getText().toString();
        EditText editTextTruthFilePath = (EditText) findViewById(R.id.file_path_truth);
        final String truthFilePath = editTextTruthFilePath.getText().toString();
        Thread worker = new Thread() {
            @Override
            public void run() {
                VideoToFile videoToFile=new VideoToFile(mHandler,truthFilePath);
                videoToFile.toFile(newFileName, videoFilePath);
            }
        };
        worker.start();
    }

    /**
     * 处理单个图片,识别二维码
     *
     * @param view 默认参数
     */
    public void processImg(View view) {
        EditText editTextVideoFilePath = (EditText) findViewById(R.id.file_path_input);
        final String imageFilePath = editTextVideoFilePath.getText().toString();
        EditText editTextTruthFilePath = (EditText) findViewById(R.id.file_path_truth);
        final String truthFilePath = editTextTruthFilePath.getText().toString();
        EditText editTextSaveFilePath = (EditText) findViewById(R.id.file_name_created);
        final String saveFilePath = editTextSaveFilePath.getText().toString();
        Thread worker = new Thread() {
            @Override
            public void run() {
                SingleImgToFile singleImgToFile=new SingleImgToFile(mHandler,truthFilePath, saveFilePath);
                singleImgToFile.singleImg(imageFilePath);
            }
        };
        worker.start();
    }

    /**
     * 在APP内打开文件
     *
     * @param view 默认参数
     */
  public void openFile(View view) {
        EditText editTextFileName = (EditText) findViewById(R.id.file_name_created);
        String originFileName = editTextFileName.getText().toString();
        File file=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),originFileName);
        file=correctFileExtension(file);
        String correctedFileName=file.getName();
        if(!correctedFileName.equals(originFileName)){
            editTextFileName.setText(correctedFileName);
        }

        String mimeType= URLConnection.guessContentTypeFromName(correctedFileName);
        if(mimeType!=null) {
            Intent newIntent=new Intent(Intent.ACTION_VIEW);
            newIntent.setDataAndType(Uri.fromFile(file), mimeType);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(newIntent);
        }else{
            new AlertDialog.Builder(this).setTitle("未识别的文件类型")
                    .setMessage("未识别的文件后缀名或文件内容")
                    .setPositiveButton("确定",null)
                    .show();
        }
    }

    private File correctFileExtension(File file){
        String originFileName = file.getName();
        int lastSeparatorIndex=originFileName.lastIndexOf('.');
        String fileNameWithoutExtension=originFileName;
        String originExtension="";
        if(lastSeparatorIndex!=-1){
            fileNameWithoutExtension=originFileName.substring(0,lastSeparatorIndex);
            originExtension=originFileName.substring(lastSeparatorIndex+1);
        }
        String correctedExtension = getFileExtension(file);
        if(!correctedExtension.equals(originExtension)){
            String correctedFileName=fileNameWithoutExtension+"."+correctedExtension;
            File newFile=new File(file.getParent(),correctedFileName);
            file.renameTo(newFile);
            file=newFile;
        }
        return file;
    }
    public String getFileExtension(File file){
        String filename = file.getName();
        return filename.substring(filename.lastIndexOf(".") + 1);

    }

    public void processCamera(View view){
        final CameraPreviewFragment fragment=new CameraPreviewFragment();
        fragment.addCallback(new CameraPreviewFragment.OnStartListener() {
            @Override
            public void onStartRecognize() {
                EditText editTextFileName = (EditText) findViewById(R.id.file_name_created);
                final String newFileName = editTextFileName.getText().toString();
                EditText editTextTruthFilePath = (EditText) findViewById(R.id.file_path_truth);
                final String truthFilePath = editTextTruthFilePath.getText().toString();
                Thread worker = new Thread() {
                    @Override
                    public void run() {
                        CameraToFile cameraToFile=new CameraToFile(mHandler,truthFilePath);
                        cameraToFile.toFile(newFileName, fragment.mPreview);
                    }
                };
                worker.start();
            }
        });

        getFragmentManager().beginTransaction().replace(R.id.left_part, fragment).addToBackStack(null).commit();
        getFragmentManager().executePendingTransactions();
    }
}
