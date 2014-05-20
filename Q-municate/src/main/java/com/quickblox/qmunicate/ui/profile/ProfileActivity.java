package com.quickblox.qmunicate.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.quickblox.internal.core.exception.BaseServiceException;
import com.quickblox.module.users.model.QBUser;
import com.quickblox.qmunicate.App;
import com.quickblox.qmunicate.R;
import com.quickblox.qmunicate.core.command.Command;
import com.quickblox.qmunicate.model.LoginType;
import com.quickblox.qmunicate.qb.commands.QBUpdateUserCommand;
import com.quickblox.qmunicate.service.QBServiceConsts;
import com.quickblox.qmunicate.ui.base.BaseActivity;
import com.quickblox.qmunicate.ui.uihelper.SimpleActionModeCallback;
import com.quickblox.qmunicate.ui.uihelper.SimpleTextWatcher;
import com.quickblox.qmunicate.ui.views.RoundedImageView;
import com.quickblox.qmunicate.utils.Consts;
import com.quickblox.qmunicate.utils.DialogUtils;
import com.quickblox.qmunicate.utils.ErrorUtils;
import com.quickblox.qmunicate.utils.GetImageFileTask;
import com.quickblox.qmunicate.utils.ImageHelper;
import com.quickblox.qmunicate.utils.OnGetFileListener;
import com.quickblox.qmunicate.utils.PrefsHelper;
import com.quickblox.qmunicate.utils.UriCreator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ProfileActivity extends BaseActivity implements OnGetFileListener {

    private LinearLayout changeAvatarLinearLayout;
    private RoundedImageView avatarImageView;
    private EditText fullNameEditText;
    private EditText emailEditText;
    private EditText statusMessageEditText;
    private ImageHelper imageHelper;
    private Bitmap avatarBitmapCurrent;
    private String fullnameCurrent;
    private String emailCurrent;
    private String fullnameOld;
    private String emailOld;
    private QBUser qbUser;
    private boolean isNeedUpdateAvatar;
    private Object actionMode;
    private boolean closeActionMode;

    public static void start(Context context) {
        Intent intent = new Intent(context, ProfileActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        useDoubleBackPressed = false;

        initUI();
        qbUser = App.getInstance().getUser();
        imageHelper = new ImageHelper(this);

        addAction(QBServiceConsts.UPDATE_USER_SUCCESS_ACTION, new UpdateUserSuccessAction());
        addAction(QBServiceConsts.UPDATE_USER_FAIL_ACTION, failAction);

        initUsersData();
        initTextChangedListeners();
    }

    private void initUI() {
        changeAvatarLinearLayout = _findViewById(R.id.changeAvatarLinearLayout);
        avatarImageView = _findViewById(R.id.avatar_imageview);
        avatarImageView.setOval(true);
        fullNameEditText = _findViewById(R.id.fullNameEditText);
        emailEditText = _findViewById(R.id.emailEditText);
        statusMessageEditText = _findViewById(R.id.statusMessageEditText);
    }

    private void initUsersData() {
        try {
            String uri;
            if (getLoginType() == LoginType.FACEBOOK) {
                changeAvatarLinearLayout.setClickable(false);
                uri = getString(R.string.inf_url_to_facebook_avatar, qbUser.getFacebookId());
                ImageLoader.getInstance().displayImage(uri, avatarImageView,
                        Consts.UIL_AVATAR_DISPLAY_OPTIONS);
            } else if (getLoginType() == LoginType.EMAIL) {
                uri = UriCreator.getUri(UriCreator.cutUid(qbUser.getWebsite()));
                ImageLoader.getInstance().displayImage(uri, avatarImageView,
                        Consts.UIL_AVATAR_DISPLAY_OPTIONS);
            }
        } catch (BaseServiceException e) {
            ErrorUtils.showError(this, e);
        }

        fullNameEditText.setText(qbUser.getFullName());
        emailEditText.setText(qbUser.getEmail());

        updateOldUserData();
    }

    private void initTextChangedListeners() {
        TextWatcher textWatcherListener = new TextWatcherListener();
        fullNameEditText.addTextChangedListener(textWatcherListener);
        emailEditText.addTextChangedListener(textWatcherListener);
    }

    private LoginType getLoginType() {
        int defValue = LoginType.EMAIL.ordinal();
        int value = App.getInstance().getPrefsHelper().getPref(PrefsHelper.PREF_LOGIN_TYPE, defValue);
        return LoginType.values()[value];
    }

    private void updateOldUserData() {
        fullnameOld = fullNameEditText.getText().toString();
        emailOld = emailEditText.getText().toString();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (actionMode != null && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            fullNameEditText.setText(qbUser.getFullName());
            emailEditText.setText(qbUser.getEmail());
            closeActionMode = true;
            ((ActionMode) actionMode).finish();
            return true;
        } else {
            closeActionMode = false;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateToParent();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            isNeedUpdateAvatar = true;
            Uri originalUri = data.getData();
            try {
                ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(originalUri, "r");
                avatarBitmapCurrent = BitmapFactory.decodeFileDescriptor(descriptor.getFileDescriptor());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            avatarImageView.setImageBitmap(avatarBitmapCurrent);
            startAction();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startAction() {
        if (actionMode != null) {
            return;
        }
        actionMode = startActionMode(new ActionModeCallback());
    }

    public void changeAvatarOnClick(View view) {
        imageHelper.getImage();
    }

    public void changeFullNameOnClick(View view) {
        initChangingEditText(fullNameEditText);
    }

    private void initChangingEditText(EditText editText) {
        editText.setEnabled(true);
        editText.requestFocus();
    }

    public void changeEmailOnClick(View view) {
        initChangingEditText(emailEditText);
    }

    @Override
    public void onGotCachedFile(File imageFile) {
        QBUpdateUserCommand.start(this, qbUser, imageFile);
    }

    @Override
    public void onGotAbsolutePathCreatedFile(String absolutePath) {

    }

    private void updateCurrentUserData() {
        fullnameCurrent = fullNameEditText.getText().toString();
        emailCurrent = emailEditText.getText().toString();
    }

    private void updateUserData() {
        if (isUserDataChanges(fullnameCurrent, emailCurrent)) {
            try {
                saveChanges(fullnameCurrent, emailCurrent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isUserDataChanges(String fullname, String email) {
        return isNeedUpdateAvatar || !fullname.equals(fullnameOld) || !email.equals(emailOld);
    }

    private void saveChanges(final String fullname, final String email) throws IOException {
        if (!isUserDataCorrect()) {
            DialogUtils.showLong(this, getString(R.string.dlg_not_all_fields_entered));
            return;
        }
        if (isUserDataChanges(fullname, email)) {
            showProgress();
            qbUser.setFullName(fullname);
            qbUser.setEmail(email);

            if (isNeedUpdateAvatar) {
                new GetImageFileTask(this).execute(imageHelper, avatarBitmapCurrent, true);
            } else {
                QBUpdateUserCommand.start(this, qbUser, null);
            }
        }
    }

    private boolean isUserDataCorrect() {
        return fullnameCurrent.length() > Consts.ZERO_VALUE && emailCurrent.length() > Consts.ZERO_VALUE;
    }

    private class TextWatcherListener extends SimpleTextWatcher {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            startAction();
        }
    }

    private class ActionModeCallback extends SimpleActionModeCallback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (!closeActionMode) {
                updateCurrentUserData();
                updateUserData();
            }
            actionMode = null;
        }
    }

    private class UpdateUserSuccessAction implements Command {

        @Override
        public void execute(Bundle bundle) {
            QBUser user = (QBUser) bundle.getSerializable(QBServiceConsts.EXTRA_USER);
            App.getInstance().setUser(user);
            updateOldUserData();
            hideProgress();
        }
    }
}