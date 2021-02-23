/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.Process;
import android.os.StrictMode;
import android.os.UserHandle;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.KeyboardShortcutGroup;
import android.view.KeyboardShortcutInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.launcher3.DropTarget.DragObject;
import com.android.launcher3.Workspace.ItemOperator;
import com.android.launcher3.accessibility.LauncherAccessibilityDelegate;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.allapps.AllAppsTransitionController;
import com.android.launcher3.allapps.DiscoveryBounce;
import com.android.launcher3.badge.BadgeInfo;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.LauncherAppsCompatVO;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.config.PlatformFeatureFlags;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.folder.FolderIconPreviewVerifier;
import com.android.launcher3.folder.FolderPagesContainer;
import com.android.launcher3.keyboard.CustomActionsPopup;
import com.android.launcher3.keyboard.ViewGroupFocusHelper;
import com.android.launcher3.logging.FileLog;
import com.android.launcher3.logging.UserEventDispatcher;
import com.android.launcher3.logging.UserEventDispatcher.UserEventDelegate;
import com.android.launcher3.model.ModelWriter;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.popup.PopupContainerWithArrow;
import com.android.launcher3.popup.PopupDataProvider;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.states.InternalStateHandler;
import com.android.launcher3.states.RotationHelper;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.uioverrides.UiFactory;
import com.android.launcher3.uioverrides.WallpaperColorInfo;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.userevent.nano.LauncherLogProto.Action;
import com.android.launcher3.userevent.nano.LauncherLogProto.ContainerType;
import com.android.launcher3.userevent.nano.LauncherLogProto.Target;
import com.android.launcher3.util.ActivityResultInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.util.MultiValueAlpha;
import com.android.launcher3.util.MultiValueAlpha.AlphaProperty;
import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.PendingRequestArgs;
import com.android.launcher3.util.SystemUiController;
import com.android.launcher3.util.Themes;
import com.android.launcher3.util.Thunk;
import com.android.launcher3.util.TraceHelper;
import com.android.launcher3.util.UiThreadHelper;
import com.android.launcher3.util.ViewOnDrawExecutor;
import com.android.launcher3.views.OptionsPopupView;
import com.android.launcher3.wallpaperpicker.WallpaperPagedView;
import com.android.launcher3.wallpaperpicker.WallpaperPicker;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.PendingAppWidgetHostView;
import com.android.launcher3.widget.WidgetAddFlowHandler;
import com.android.launcher3.widget.WidgetHostViewLoader;
import com.android.launcher3.widget.WidgetListRowEntry;
import com.android.launcher3.widget.WidgetsFullSheet;
import com.android.launcher3.widget.custom.CustomWidgetParser;
import com.condor.launcher.blur.BlurFactory;
import com.condor.launcher.editmode.EditModePanel;
import com.condor.launcher.unreadnotifier.UnreadInfoManager;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import static android.content.pm.ActivityInfo.CONFIG_ORIENTATION;
import static android.content.pm.ActivityInfo.CONFIG_SCREEN_SIZE;
import static com.android.launcher3.LauncherAnimUtils.SPRING_LOADED_EXIT_DELAY;
import static com.android.launcher3.LauncherState.ALL_APPS;
import static com.android.launcher3.LauncherState.EDIT_MODE;
import static com.android.launcher3.LauncherState.IMPORT_MODE;
import static com.android.launcher3.LauncherState.NORMAL;
import static com.android.launcher3.LauncherState.SPRING_LOADED;
import static com.android.launcher3.allapps.AllAppsContainerView.State.FROZEN;
import static com.android.launcher3.dragndrop.DragLayer.ALPHA_INDEX_LAUNCHER_LOAD;
import static com.android.launcher3.logging.LoggerUtils.newContainerTarget;
import static com.android.launcher3.logging.LoggerUtils.newTarget;

/**
 * Default launcher application.
 */
public class Launcher extends BaseDraggingActivity implements LauncherExterns,
        LauncherModel.Callbacks, LauncherProviderChangeListener, UserEventDelegate, WallpaperColorInfo.OnThemeChangeListener {
    public static final String TAG = "Launcher";
    static final boolean LOGD = false;

    static final boolean DEBUG_STRICT_MODE = false;

    private static final int REQUEST_CREATE_SHORTCUT = 1;
    private static final int REQUEST_CREATE_APPWIDGET = 5;

    private static final int REQUEST_PICK_APPWIDGET = 9;

    private static final int REQUEST_BIND_APPWIDGET = 11;
    public static final int REQUEST_BIND_PENDING_APPWIDGET = 12;
    public static final int REQUEST_RECONFIGURE_APPWIDGET = 13;

    private static final int REQUEST_PERMISSION_CALL_PHONE = 14;

    private static final float BOUNCE_ANIMATION_TENSION = 1.3f;

    /**
     * IntentStarter uses request codes starting with this. This must be greater than all activity
     * request codes used internally.
     */
    protected static final int REQUEST_LAST = 100;

    // Type: int
    private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    // Type: int
    private static final String RUNTIME_STATE = "launcher.state";
    // Type: PendingRequestArgs
    private static final String RUNTIME_STATE_PENDING_REQUEST_ARGS = "launcher.request_args";
    // Type: ActivityResultInfo
    private static final String RUNTIME_STATE_PENDING_ACTIVITY_RESULT = "launcher.activity_result";
    // Type: SparseArray<Parcelable>
    private static final String RUNTIME_STATE_WIDGET_PANEL = "launcher.widget_panel";

    private LauncherStateManager mStateManager;

    private static final int ON_ACTIVITY_RESULT_ANIMATION_DELAY = 500;

    // How long to wait before the new-shortcut animation automatically pans the workspace
    private static final int NEW_APPS_PAGE_MOVE_DELAY = 500;
    private static final int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 5;
    @Thunk static final int NEW_APPS_ANIMATION_DELAY = 500;

    private LauncherAppTransitionManager mAppTransitionManager;
    private Configuration mOldConfig;

    @Thunk Workspace mWorkspace;
    private View mLauncherView;
    @Thunk DragLayer mDragLayer;
    private DragController mDragController;

    private AppWidgetManagerCompat mAppWidgetManager;
    private LauncherAppWidgetHost mAppWidgetHost;

    private final int[] mTmpAddItemCellCoordinates = new int[2];

    @Thunk Hotseat mHotseat;
    @Nullable private View mHotseatSearchBox;

    private DropTargetBar mDropTargetBar;

    // Main container view for the all apps screen.
    @Thunk AllAppsContainerView mAppsView;
    AllAppsTransitionController mAllAppsController;

    // UI and state for the overview panel
    private View mOverviewPanel;

    @Thunk boolean mWorkspaceLoading = true;

    private OnResumeCallback mOnResumeCallback;

    private ViewOnDrawExecutor mPendingExecutor;

    private LauncherModel mModel;
    private ModelWriter mModelWriter;
    private IconCache mIconCache;
    private LauncherAccessibilityDelegate mAccessibilityDelegate;

    private PopupDataProvider mPopupDataProvider;

    private int mSynchronouslyBoundPage = PagedView.INVALID_PAGE;

    // We only want to get the SharedPreferences once since it does an FS stat each time we get
    // it from the context.
    private SharedPreferences mSharedPrefs;

    // Activity result which needs to be processed after workspace has loaded.
    private ActivityResultInfo mPendingActivityResult;
    /**
     * Holds extra information required to handle a result from an external call, like
     * {@link #startActivityForResult(Intent, int)} or {@link #requestPermissions(String[], int)}
     */
    private PendingRequestArgs mPendingRequestArgs;

    public ViewGroupFocusHelper mFocusHandler;

    private RotationHelper mRotationHelper;


    private final Handler mHandler = new Handler();
    private final Runnable mLogOnDelayedResume = this::logOnDelayedResume;

    //liuzuo:add the background of workspace when opening folder:begin
    Animator mAniWorkspaceBg;
    Bitmap mBlur;
    ImageView mWorkspaceBgOpenFolder;
    private Runnable wallpaperChangeRunnable;
    public static final int REQUEST_PERMISSION_ALL = 0;
    public static String[] sAllPermissions = new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    //liuzuo:add the background of workspace when opening folder:end
    //Bruce : add for unread message : begin
    public static final int REQUEST_PERMISSION_UNREAD = 4212;
    public static String[] sUreadPermissions = new String[]{
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG};
    //Bruce : add for unread message : end
    //liuzuo:add for addIcon:begin
    private boolean mOpenFolder;
    private FolderIcon mEditFolderIcon;
    private Button mFolderImportButton;
    private FolderInfo mEditFolderInfo;
    LinearLayout mFolderImportContainer;
    public ArrayList<ShortcutInfo> mCheckedShortcutInfos = new ArrayList<ShortcutInfo>();
    public ArrayList<BubbleTextView> mCheckedBubbleTextViews = new ArrayList<BubbleTextView>();
    public HashSet<FolderInfo> mCheckedFolderInfos = new HashSet<FolderInfo>();
    public HashSet<FolderIcon> mCheckedFolderIcons = new HashSet<FolderIcon>();
    Animator animImportButton;
    private boolean isSuccessAddIcon;
    private boolean exitImportModeInHomeKey;
    //liuzuo:add for addIcon:end

    //liuzuo:add for wallpaper:begin
    // Perry: Optimizing wallpaper picker UI: start
    private WallpaperPagedView mWallpaperPicker;
    // Perry: Optimizing wallpaper picker UI: end
    // Perry: Add edit mode panel: start
    private EditModePanel mEditModePanel;
    // Perry: Add edit mode panel: end

    //liuzuo:add for wallpaper:end


    // liuzuo: Add  for ace folder: start
    private FolderPagesContainer mFolderPagesContainer;
    // liuzuo: Add  for ace folder: end
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG_STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        TraceHelper.beginSection("Launcher-onCreate");

        super.onCreate(savedInstanceState);
        //liuzuo:add the background of workspace when opening folder:begin
        WallpaperColorInfo wallpaperColorInfo = WallpaperColorInfo.getInstance(this);
        wallpaperColorInfo.setOnThemeChangeListener(this);
        //liuzuo:add the background of workspace when opening folder:end
        TraceHelper.partitionSection("Launcher-onCreate", "super call");

        LauncherAppState app = LauncherAppState.getInstance(this);
        mOldConfig = new Configuration(getResources().getConfiguration());
        mModel = app.setLauncher(this);
        initDeviceProfile(app.getInvariantDeviceProfile());

        mSharedPrefs = Utilities.getPrefs(this);
        mIconCache = app.getIconCache();
        mAccessibilityDelegate = new LauncherAccessibilityDelegate(this);

        mDragController = new DragController(this);
        mAllAppsController = new AllAppsTransitionController(this);
        mStateManager = new LauncherStateManager(this);
        UiFactory.onCreate(this);

        mAppWidgetManager = AppWidgetManagerCompat.getInstance(this);

        mAppWidgetHost = new LauncherAppWidgetHost(this);
        mAppWidgetHost.startListening();

        mLauncherView = LayoutInflater.from(this).inflate(R.layout.launcher, null);

        setupViews();
        mPopupDataProvider = new PopupDataProvider(this);

        mRotationHelper = new RotationHelper(this);
        mAppTransitionManager = LauncherAppTransitionManager.newInstance(this);

        boolean internalStateHandled = InternalStateHandler.handleCreate(this, getIntent());
        if (internalStateHandled) {
            if (savedInstanceState != null) {
                // InternalStateHandler has already set the appropriate state.
                // We dont need to do anything.
                savedInstanceState.remove(RUNTIME_STATE);
            }
        }
        restoreState(savedInstanceState);

        // We only load the page synchronously if the user rotates (or triggers a
        // configuration change) while launcher is in the foreground
        int currentScreen = PagedView.INVALID_RESTORE_PAGE;
        if (savedInstanceState != null) {
            currentScreen = savedInstanceState.getInt(RUNTIME_STATE_CURRENT_SCREEN, currentScreen);
        }

        if (!mModel.startLoader(currentScreen)) {
            if (!internalStateHandled) {
                // If we are not binding synchronously, show a fade in animation when
                // the first page bind completes.
                mDragLayer.getAlphaProperty(ALPHA_INDEX_LAUNCHER_LOAD).setValue(0);
            }
        } else {
            // Pages bound synchronously.
            mWorkspace.setCurrentPage(currentScreen);

            setWorkspaceLoading(true);
        }

        // For handling default keys
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        setContentView(mLauncherView);
        getRootView().dispatchInsets();

        // Listen for broadcasts
        registerReceiver(mScreenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        getSystemUiController().updateUiState(SystemUiController.UI_STATE_BASE_WINDOW,
                Themes.getAttrBoolean(this, R.attr.isWorkspaceDarkText));

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onCreate(savedInstanceState);
        }
        mRotationHelper.initialize();

        TraceHelper.endSection("Launcher-onCreate");
        checkPermission();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        int diff = newConfig.diff(mOldConfig);
        if ((diff & (CONFIG_ORIENTATION | CONFIG_SCREEN_SIZE)) != 0) {
            mUserEventDispatcher = null;
            initDeviceProfile(mDeviceProfile.inv);
            dispatchDeviceProfileChanged();
            reapplyUi();
            mDragLayer.recreateControllers();

            // TODO: We can probably avoid rebind when only screen size changed.
            rebindModel();
        }

        mOldConfig.setTo(newConfig);
        UiFactory.onLauncherStateOrResumeChanged(this);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void reapplyUi() {
        getRootView().dispatchInsets();
        // Perry: Adjust UI: start
        getRootView().post(()-> {
            getStateManager().reapplyState(true /* cancelCurrentAnimation */);
        });
        // Perry: Adjust UI: end
    }

    @Override
    public void rebindModel() {
        int currentPage = mWorkspace.getNextPage();
        if (mModel.startLoader(currentPage)) {
            mWorkspace.setCurrentPage(currentPage);
            setWorkspaceLoading(true);
        }
    }

    private void initDeviceProfile(InvariantDeviceProfile idp) {
        // Load configuration-specific DeviceProfile
        mDeviceProfile = idp.getDeviceProfile(this);
        if (isInMultiWindowModeCompat()) {
            Display display = getWindowManager().getDefaultDisplay();
            Point mwSize = new Point();
            display.getSize(mwSize);
            mDeviceProfile = mDeviceProfile.getMultiWindowProfile(this, mwSize);
        }
        onDeviceProfileInitiated();
        mModelWriter = mModel.getWriter(mDeviceProfile.isVerticalBarLayout(), true);
    }

    public RotationHelper getRotationHelper() {
        return mRotationHelper;
    }

    public LauncherStateManager getStateManager() {
        return mStateManager;
    }

    @Override
    public <T extends View> T findViewById(int id) {
        return mLauncherView.findViewById(id);
    }

    @Override
    public void onAppWidgetHostReset() {
        if (mAppWidgetHost != null) {
            mAppWidgetHost.startListening();
        }
    }

    private LauncherCallbacks mLauncherCallbacks;

    /**
     * Call this after onCreate to set or clear overlay.
     */
    public void setLauncherOverlay(LauncherOverlay overlay) {
        if (overlay != null) {
            overlay.setOverlayCallbacks(new LauncherOverlayCallbacksImpl());
        }
        mWorkspace.setLauncherOverlay(overlay);
        // Perry: Optimizing page indicator: start
        mWorkspace.updatePageIndicatorState();
        // Perry: Optimizing page indicator: end
    }

    public boolean setLauncherCallbacks(LauncherCallbacks callbacks) {
        mLauncherCallbacks = callbacks;
        return true;
    }

    @Override
    public void onLauncherProviderChanged() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onLauncherProviderChange();
        }
    }

    public boolean isDraggingEnabled() {
        // We prevent dragging when we are loading the workspace as it is possible to pick up a view
        // that is subsequently removed from the workspace in startBinding().
        return !isWorkspaceLoading();
    }

    public int getViewIdForItem(ItemInfo info) {
        // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
        // This cast is safe as long as the id < 0x00FFFFFF
        // Since we jail all the dynamically generated views, there should be no clashes
        // with any other views.
        return (int) info.id;
    }

    public PopupDataProvider getPopupDataProvider() {
        return mPopupDataProvider;
    }

    @Override
    public BadgeInfo getBadgeInfoForItem(ItemInfo info) {
        return mPopupDataProvider.getBadgeInfoForItem(info);
    }

    @Override
    public void invalidateParent(ItemInfo info) {
        FolderIconPreviewVerifier verifier = new FolderIconPreviewVerifier(getDeviceProfile().inv);
        if (verifier.isItemInPreview(info.rank) && (info.container >= 0)) {
            View folderIcon = getWorkspace().getHomescreenIconByItemId(info.container);
            if (folderIcon != null) {
                folderIcon.invalidate();
            }
        }
    }

    /**
     * Returns whether we should delay spring loaded mode -- for shortcuts and widgets that have
     * a configuration step, this allows the proper animations to run after other transitions.
     */
    private long completeAdd(
            int requestCode, Intent intent, int appWidgetId, PendingRequestArgs info) {
        long screenId = info.screenId;
        if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            // When the screen id represents an actual screen (as opposed to a rank) we make sure
            // that the drop page actually exists.
            screenId = ensurePendingDropLayoutExists(info.screenId);
        }

        switch (requestCode) {
            case REQUEST_CREATE_SHORTCUT:
                completeAddShortcut(intent, info.container, screenId, info.cellX, info.cellY, info);
                break;
            case REQUEST_CREATE_APPWIDGET:
                completeAddAppWidget(appWidgetId, info, null, null);
                break;
            case REQUEST_RECONFIGURE_APPWIDGET:
                completeRestoreAppWidget(appWidgetId, LauncherAppWidgetInfo.RESTORE_COMPLETED);
                break;
            case REQUEST_BIND_PENDING_APPWIDGET: {
                int widgetId = appWidgetId;
                LauncherAppWidgetInfo widgetInfo =
                        completeRestoreAppWidget(widgetId, LauncherAppWidgetInfo.FLAG_UI_NOT_READY);
                if (widgetInfo != null) {
                    // Since the view was just bound, also launch the configure activity if needed
                    LauncherAppWidgetProviderInfo provider = mAppWidgetManager
                            .getLauncherAppWidgetInfo(widgetId);
                    if (provider != null) {
                        new WidgetAddFlowHandler(provider)
                                .startConfigActivity(this, widgetInfo, REQUEST_RECONFIGURE_APPWIDGET);
                    }
                }
                break;
            }
        }

        return screenId;
    }

    private void handleActivityResult(
            final int requestCode, final int resultCode, final Intent data) {
        if (isWorkspaceLoading()) {
            // process the result once the workspace has loaded.
            mPendingActivityResult = new ActivityResultInfo(requestCode, resultCode, data);
            return;
        }
        mPendingActivityResult = null;

        // Reset the startActivity waiting flag
        final PendingRequestArgs requestArgs = mPendingRequestArgs;
        setWaitingForResult(null);
        if (requestArgs == null) {
            return;
        }

        final int pendingAddWidgetId = requestArgs.getWidgetId();

        Runnable exitSpringLoaded = new Runnable() {
            @Override
            public void run() {
                // Perry: Add edit mode panel: start
                mStateManager.goToStateFrom(NORMAL, SPRING_LOADED, SPRING_LOADED_EXIT_DELAY);
                // Perry: Add edit mode panel: end
            }
        };

        if (requestCode == REQUEST_BIND_APPWIDGET) {
            // This is called only if the user did not previously have permissions to bind widgets
            final int appWidgetId = data != null ?
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (resultCode == RESULT_CANCELED) {
                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId, requestArgs);
                mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
            } else if (resultCode == RESULT_OK) {
                addAppWidgetImpl(
                        appWidgetId, requestArgs, null,
                        requestArgs.getWidgetHandler(),
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY);
            }
            return;
        }

        boolean isWidgetDrop = (requestCode == REQUEST_PICK_APPWIDGET ||
                requestCode == REQUEST_CREATE_APPWIDGET);

        // We have special handling for widgets
        if (isWidgetDrop) {
            final int appWidgetId;
            int widgetId = data != null ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                    : -1;
            if (widgetId < 0) {
                appWidgetId = pendingAddWidgetId;
            } else {
                appWidgetId = widgetId;
            }

            final int result;
            if (appWidgetId < 0 || resultCode == RESULT_CANCELED) {
                Log.e(TAG, "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not " +
                        "returned from the widget configuration activity.");
                result = RESULT_CANCELED;
                completeTwoStageWidgetDrop(result, appWidgetId, requestArgs);
                final Runnable onComplete = new Runnable() {
                    @Override
                    public void run() {
                        // Perry: Add edit mode panel: start
                        getStateManager().goToStateFrom(NORMAL, SPRING_LOADED);
                        // Perry: Add edit mode panel: end
                    }
                };

                mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
            } else {
                if (requestArgs.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    // When the screen id represents an actual screen (as opposed to a rank)
                    // we make sure that the drop page actually exists.
                    requestArgs.screenId =
                            ensurePendingDropLayoutExists(requestArgs.screenId);
                }
                final CellLayout dropLayout =
                        mWorkspace.getScreenWithId(requestArgs.screenId);

                dropLayout.setDropPending(true);
                final Runnable onComplete = new Runnable() {
                    @Override
                    public void run() {
                        completeTwoStageWidgetDrop(resultCode, appWidgetId, requestArgs);
                        dropLayout.setDropPending(false);
                    }
                };
                mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
            }
            return;
        }

        if (requestCode == REQUEST_RECONFIGURE_APPWIDGET
                || requestCode == REQUEST_BIND_PENDING_APPWIDGET) {
            if (resultCode == RESULT_OK) {
                // Update the widget view.
                completeAdd(requestCode, data, pendingAddWidgetId, requestArgs);
            }
            // Leave the widget in the pending state if the user canceled the configure.
            return;
        }

        if (requestCode == REQUEST_CREATE_SHORTCUT) {
            // Handle custom shortcuts created using ACTION_CREATE_SHORTCUT.
            if (resultCode == RESULT_OK && requestArgs.container != ItemInfo.NO_ID) {
                completeAdd(requestCode, data, -1, requestArgs);
                mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);

            } else if (resultCode == RESULT_CANCELED) {
                mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
            }
        }
        mDragLayer.clearAnimatedView();
    }

    @Override
    public void onActivityResult(
            final int requestCode, final int resultCode, final Intent data) {
        handleActivityResult(requestCode, resultCode, data);
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        PendingRequestArgs pendingArgs = mPendingRequestArgs;
        if (requestCode == REQUEST_PERMISSION_CALL_PHONE && pendingArgs != null
                && pendingArgs.getRequestCode() == REQUEST_PERMISSION_CALL_PHONE) {
            setWaitingForResult(null);

            View v = null;
            CellLayout layout = getCellLayout(pendingArgs.container, pendingArgs.screenId);
            if (layout != null) {
                v = layout.getChildAt(pendingArgs.cellX, pendingArgs.cellY);
            }
            Intent intent = pendingArgs.getPendingIntent();

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivitySafely(v, intent, null);
            } else {
                // TODO: Show a snack bar with link to settings
                Toast.makeText(this, getString(R.string.msg_no_phone_permission,
                        getString(R.string.derived_app_name)), Toast.LENGTH_SHORT).show();
            }
        }
        //liuzuo:add for folder bg:begin
        else if (requestCode == REQUEST_PERMISSION_ALL) {
            setWallPaperBlur(null, false);
        } else if (requestCode == REQUEST_PERMISSION_UNREAD) {
            //Bruce : add for unread message : begin
            if (grantResults.length > 0) {
                boolean isInit = false;
                for (int i = 0; i < grantResults.length; i++) {
                    isInit = isInit || (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                }
                if (isInit) {
                    UnreadInfoManager.getInstance(this).init();
                }
            }
        }
        //Bruce : add for unread message : end
        //liuzuo:add for folder bg:end
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onRequestPermissionsResult(requestCode, permissions,
                    grantResults);
        }

        // Perry: Has no translation animation on first run: start
        clearForceInvisibleFlag(INVISIBLE_ALL);
        // Perry: Has no translation animation on first run: end
    }

    /**
     * Check to see if a given screen id exists. If not, create it at the end, return the new id.
     *
     * @param screenId the screen id to check
     * @return the new screen, or screenId if it exists
     */
    private long ensurePendingDropLayoutExists(long screenId) {
        CellLayout dropLayout = mWorkspace.getScreenWithId(screenId);
        if (dropLayout == null) {
            // it's possible that the add screen was removed because it was
            // empty and a re-bind occurred
            mWorkspace.addExtraEmptyScreen();
            return mWorkspace.commitExtraEmptyScreen();
        } else {
            return screenId;
        }
    }

    @Thunk void completeTwoStageWidgetDrop(
            final int resultCode, final int appWidgetId, final PendingRequestArgs requestArgs) {
        CellLayout cellLayout = mWorkspace.getScreenWithId(requestArgs.screenId);
        Runnable onCompleteRunnable = null;
        int animationType = 0;

        AppWidgetHostView boundWidget = null;
        if (resultCode == RESULT_OK) {
            animationType = Workspace.COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION;
            final AppWidgetHostView layout = mAppWidgetHost.createView(this, appWidgetId,
                    requestArgs.getWidgetHandler().getProviderInfo(this));
            boundWidget = layout;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    completeAddAppWidget(appWidgetId, requestArgs, layout, null);
                    // Perry: Add edit mode panel: start
                    mStateManager.goToStateFrom(NORMAL, SPRING_LOADED, SPRING_LOADED_EXIT_DELAY);
                    // Perry: Add edit mode panel: end
                }
            };
        } else if (resultCode == RESULT_CANCELED) {
            mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            animationType = Workspace.CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION;
        }
        if (mDragLayer.getAnimatedView() != null) {
            mWorkspace.animateWidgetDrop(requestArgs, cellLayout,
                    (DragView) mDragLayer.getAnimatedView(), onCompleteRunnable,
                    animationType, boundWidget, true);
        } else if (onCompleteRunnable != null) {
            // The animated view may be null in the case of a rotation during widget configuration
            onCompleteRunnable.run();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirstFrameAnimatorHelper.setIsVisible(false);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onStop();
        }
        getUserEventDispatcher().logActionCommand(Action.Command.STOP,
                mStateManager.getState().containerType, -1);

        mAppWidgetHost.setListenIfResumed(false);

        NotificationListener.removeNotificationsChangedListener();
        getStateManager().moveToRestState();

        UiFactory.onLauncherStateOrResumeChanged(this);

        // Workaround for b/78520668, explicitly trim memory once UI is hidden
        onTrimMemory(TRIM_MEMORY_UI_HIDDEN);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirstFrameAnimatorHelper.setIsVisible(true);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onStart();
        }
        mAppWidgetHost.setListenIfResumed(true);
        NotificationListener.setNotificationsChangedListener(mPopupDataProvider);
        UiFactory.onStart(this);
    }

    private void logOnDelayedResume() {
        if (hasBeenResumed()) {
            getUserEventDispatcher().logActionCommand(Action.Command.RESUME,
                    mStateManager.getState().containerType, -1);
            getUserEventDispatcher().startSession();
        }
    }

    @Override
    protected void onResume() {
        TraceHelper.beginSection("ON_RESUME");
        super.onResume();
        TraceHelper.partitionSection("ON_RESUME", "superCall");

        mHandler.removeCallbacks(mLogOnDelayedResume);
        Utilities.postAsyncCallback(mHandler, mLogOnDelayedResume);

        setOnResumeCallback(null);
        // Process any items that were added while Launcher was away.
        InstallShortcutReceiver.disableAndFlushInstallQueue(
                InstallShortcutReceiver.FLAG_ACTIVITY_PAUSED, this);

        // Refresh shortcuts if the permission changed.
        mModel.refreshShortcutsIfRequired();

        DiscoveryBounce.showForHomeIfNeeded(this);
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onResume();
        }
        UiFactory.onLauncherStateOrResumeChanged(this);

        TraceHelper.endSection("ON_RESUME");
    }

    @Override
    protected void onPause() {
        // Ensure that items added to Launcher are queued until Launcher returns
        InstallShortcutReceiver.enableInstallQueue(InstallShortcutReceiver.FLAG_ACTIVITY_PAUSED);

        super.onPause();
        mDragController.cancelDrag();
        mDragController.resetLastGestureUpTime();

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onPause();
        }
        //liuzuo:add for addicon:begin
        if(getImportMode()) {
            forceExitImportMode();
        }
        //liuzuo:add for addicon:end
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        UiFactory.onLauncherStateOrResumeChanged(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mStateManager.onWindowFocusChanged();
    }
    //liuzuo:add the background of workspace when opening folder:begin
    @Override
    public void wallpaperChange() {
        synchronized (this) {
            if (!isWallpaperMode()) {
                wallpaperChangeRunnable = null;
                setWallPaperBlur(null, false);
            } else {
                wallpaperChangeRunnable = () -> setWallPaperBlur(null, false);
                wallpaperChangeRunnable.run();
            }
        }
    }
    public boolean isWallpaperMode() {
        if(mStateManager==null){
            return true;
        }
        return (mStateManager.getState() == LauncherState.EDIT_MODE) || (mStateManager.getRestState() == LauncherState.EDIT_MODE && mWallpaperPicker != null);
    }
    //liuzuo:add the background of workspace when opening folder:end
    public interface LauncherOverlay {

        /**
         * Touch interaction leading to overscroll has begun
         */
        void onScrollInteractionBegin();

        /**
         * Touch interaction related to overscroll has ended
         */
        void onScrollInteractionEnd();

        /**
         * Scroll progress, between 0 and 100, when the user scrolls beyond the leftmost
         * screen (or in the case of RTL, the rightmost screen).
         */
        void onScrollChange(float progress, boolean rtl);

        /**
         * Called when the launcher is ready to use the overlay
         * @param callbacks A set of callbacks provided by Launcher in relation to the overlay
         */
        void setOverlayCallbacks(LauncherOverlayCallbacks callbacks);
    }

    public interface LauncherOverlayCallbacks {
        void onScrollChanged(float progress);
    }

    class LauncherOverlayCallbacksImpl implements LauncherOverlayCallbacks {

        public void onScrollChanged(float progress) {
            if (mWorkspace != null) {
                mWorkspace.onOverlayScrollChanged(progress);
            }
        }
    }

    public boolean hasSettings() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.hasSettings();
        } else {
            // On O and above we there is always some setting present settings (add icon to
            // home screen or icon badging). On earlier APIs we will have the allow rotation
            // setting, on devices with a locked orientation,
            return Utilities.ATLEAST_OREO || !getResources().getBoolean(R.bool.allow_rotation);
        }
    }

    public boolean isInState(LauncherState state) {
        return mStateManager.getState() == state;
    }

    /**
     * Restores the previous state, if it exists.
     *
     * @param savedState The previous state.
     */
    private void restoreState(Bundle savedState) {
        if (savedState == null) {
            return;
        }

        int stateOrdinal = savedState.getInt(RUNTIME_STATE, NORMAL.ordinal);
        LauncherState[] stateValues = LauncherState.values();
        LauncherState state = stateValues[stateOrdinal];
        if (!state.disableRestore) {
            // Perry: Icon shape override for different desktop modes: start
            if (state == EDIT_MODE) {
                mStateManager.goToState(NORMAL, false);
            } else {
                mStateManager.goToState(state, false /* animated */);
            }
            // Perry: Icon shape override for different desktop modes: end
        }

        PendingRequestArgs requestArgs = savedState.getParcelable(RUNTIME_STATE_PENDING_REQUEST_ARGS);
        if (requestArgs != null) {
            setWaitingForResult(requestArgs);
        }

        mPendingActivityResult = savedState.getParcelable(RUNTIME_STATE_PENDING_ACTIVITY_RESULT);

        SparseArray<Parcelable> widgetsState =
                savedState.getSparseParcelableArray(RUNTIME_STATE_WIDGET_PANEL);
        if (widgetsState != null) {
            WidgetsFullSheet.show(this, false).restoreHierarchyState(widgetsState);
        }
    }

    /**
     * Finds all the views we need and configure them properly.
     */
    private void setupViews() {
        if(PlatformFeatureFlags.SUPPORT_FOLDER_TAP_MODE){
            mFolderPagesContainer = FolderPagesContainer.create(this);
        }
        mDragLayer = findViewById(R.id.drag_layer);
        mFocusHandler = mDragLayer.getFocusIndicatorHelper();
        mWorkspace = mDragLayer.findViewById(R.id.workspace);
        mWorkspace.initParentViews(mDragLayer);
        mOverviewPanel = findViewById(R.id.overview_panel);
        mHotseat = findViewById(R.id.hotseat);
        mHotseatSearchBox = findViewById(R.id.search_container_hotseat);

        mLauncherView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        // Setup the drag layer
        mDragLayer.setup(mDragController, mWorkspace);
        UiFactory.setOnTouchControllersChangedListener(this, mDragLayer::recreateControllers);

        mWorkspace.setup(mDragController);
        // Until the workspace is bound, ensure that we keep the wallpaper offset locked to the
        // default state, otherwise we will update to the wrong offsets in RTL
        mWorkspace.lockWallpaperToDefaultPage();
        mWorkspace.bindAndInitFirstWorkspaceScreen(null /* recycled qsb */);
        mDragController.addDragListener(mWorkspace);

        // Get the search/delete/uninstall bar
        mDropTargetBar = mDragLayer.findViewById(R.id.drop_target_bar);

        // Setup Apps
        mAppsView = findViewById(R.id.apps_view);

        // Setup the drag controller (drop targets have to be added in reverse order in priority)
        mDragController.setMoveTarget(mWorkspace);
        mDropTargetBar.setup(mDragController);

        mAllAppsController.setupViews(mAppsView);
        //liuzuo:add for addIcon:begin
        mFolderImportContainer = (LinearLayout) findViewById(R.id.folder_importmode_button_container);
        mFolderImportButton=(Button)findViewById(R.id.folder_importmode_button);
        mFolderImportButton.setOnClickListener(ItemClickHandler.INSTANCE);
        mFolderImportButton.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                v.getBackground().setAlpha(127);
            }else if(event.getAction() == MotionEvent.ACTION_UP){
                v.getBackground().setAlpha(226);
            }else if(event.getAction() == MotionEvent.ACTION_CANCEL){
                v.getBackground().setAlpha(226);
            }
            return false;

        });

        //liuzuo:add for addIcon:end
        //liuzuo:add the background of workspace when opening folder:begin
        mWorkspaceBgOpenFolder = (ImageView) findViewById(R.id.img_workspace_bg_folder);
        setWallPaperBlur(null, false);
        //liuzuo:add the background of workspace when opening folder:end

        //liuzuo:add for wallpaper:begin
        // Perry: Optimizing wallpaper picker UI: start
        mWallpaperPicker = (WallpaperPagedView) findViewById(R.id.wallpaper_picker);
        // Perry: Optimizing wallpaper picker UI: end
        //liuzuo:add for wallpaper:end

        // Perry: Add edit mode panel: start
        mEditModePanel = (EditModePanel) findViewById(R.id.editmode_panel);
        // Perry: Add edit mode panel: end
    }

    /**
     * Creates a view representing a shortcut.
     *
     * @param info The data structure describing the shortcut.
     */
    View createShortcut(ShortcutInfo info) {
        return createShortcut((ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentPage()), info);
    }

    /**
     * Creates a view representing a shortcut inflated from the specified resource.
     *
     * @param parent The group the shortcut belongs to.
     * @param info The data structure describing the shortcut.
     *
     * @return A View inflated from layoutResId.
     */
    public View createShortcut(ViewGroup parent, ShortcutInfo info) {
        BubbleTextView favorite = (BubbleTextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_icon, parent, false);
        favorite.applyFromShortcutInfo(info);
        favorite.setOnClickListener(ItemClickHandler.INSTANCE);
        favorite.setOnFocusChangeListener(mFocusHandler);
        return favorite;
    }

    /**
     * Add a shortcut to the workspace or to a Folder.
     *
     * @param data The intent describing the shortcut.
     */
    private void completeAddShortcut(Intent data, long container, long screenId, int cellX,
            int cellY, PendingRequestArgs args) {
        if (args.getRequestCode() != REQUEST_CREATE_SHORTCUT
                || args.getPendingIntent().getComponent() == null) {
            return;
        }

        int[] cellXY = mTmpAddItemCellCoordinates;
        CellLayout layout = getCellLayout(container, screenId);

        ShortcutInfo info = null;
        if (Utilities.ATLEAST_OREO) {
            info = LauncherAppsCompatVO.createShortcutInfoFromPinItemRequest(
                    this, LauncherAppsCompatVO.getPinItemRequest(data), 0);
        }

        if (info == null) {
            // Legacy shortcuts are only supported for primary profile.
            info = Process.myUserHandle().equals(args.user)
                    ? InstallShortcutReceiver.fromShortcutIntent(this, data) : null;

            if (info == null) {
                Log.e(TAG, "Unable to parse a valid custom shortcut result");
                return;
            } else if (!new PackageManagerHelper(this).hasPermissionForActivity(
                    info.intent, args.getPendingIntent().getComponent().getPackageName())) {
                // The app is trying to add a shortcut without sufficient permissions
                Log.e(TAG, "Ignoring malicious intent " + info.intent.toUri(0));
                return;
            }
        }

        if (container < 0) {
            // Adding a shortcut to the Workspace.
            final View view = createShortcut(info);
            boolean foundCellSpan = false;
            // First we check if we already know the exact location where we want to add this item.
            if (cellX >= 0 && cellY >= 0) {
                cellXY[0] = cellX;
                cellXY[1] = cellY;
                foundCellSpan = true;

                // If appropriate, either create a folder or add to an existing folder
                if (mWorkspace.createUserFolderIfNecessary(view, container, layout, cellXY, 0,
                        true, null)) {
                    return;
                }
                DragObject dragObject = new DragObject();
                dragObject.dragInfo = info;
                if (mWorkspace.addToExistingFolderIfNecessary(view, layout, cellXY, 0, dragObject,
                        true)) {
                    return;
                }
            } else {
                foundCellSpan = layout.findCellForSpan(cellXY, 1, 1);
            }

            if (!foundCellSpan) {
                mWorkspace.onNoCellFound(layout);
                return;
            }

            getModelWriter().addItemToDatabase(info, container, screenId, cellXY[0], cellXY[1]);
            mWorkspace.addInScreen(view, info);
        } else {
            // Adding a shortcut to a Folder.
            FolderIcon folderIcon = findFolderIcon(container);
            if (folderIcon != null) {
                FolderInfo folderInfo = (FolderInfo) folderIcon.getTag();
                folderInfo.add(info, args.rank, false);
            } else {
                Log.e(TAG, "Could not find folder with id " + container + " to add shortcut.");
            }
        }
    }

    public FolderIcon findFolderIcon(final long folderIconId) {
        return (FolderIcon) mWorkspace.getFirstMatch(new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View view) {
                return info != null && info.id == folderIconId;
            }
        });
    }

    /**
     * Add a widget to the workspace.
     *
     * @param appWidgetId The app widget id
     */
    @Thunk void completeAddAppWidget(int appWidgetId, ItemInfo itemInfo,
            AppWidgetHostView hostView, LauncherAppWidgetProviderInfo appWidgetInfo) {

        if (appWidgetInfo == null) {
            appWidgetInfo = mAppWidgetManager.getLauncherAppWidgetInfo(appWidgetId);
        }

        LauncherAppWidgetInfo launcherInfo;
        launcherInfo = new LauncherAppWidgetInfo(appWidgetId, appWidgetInfo.provider);
        launcherInfo.spanX = itemInfo.spanX;
        launcherInfo.spanY = itemInfo.spanY;
        launcherInfo.minSpanX = itemInfo.minSpanX;
        launcherInfo.minSpanY = itemInfo.minSpanY;
        launcherInfo.user = appWidgetInfo.getProfile();

        getModelWriter().addItemToDatabase(launcherInfo,
                itemInfo.container, itemInfo.screenId, itemInfo.cellX, itemInfo.cellY);

        if (hostView == null) {
            // Perform actual inflation because we're live
            hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
        }
        hostView.setVisibility(View.VISIBLE);
        prepareAppWidget(hostView, launcherInfo);
        mWorkspace.addInScreen(hostView, launcherInfo);
    }

    private void prepareAppWidget(AppWidgetHostView hostView, LauncherAppWidgetInfo item) {
        hostView.setTag(item);
        item.onBindAppWidget(this, hostView);
        hostView.setFocusable(true);
        hostView.setOnFocusChangeListener(mFocusHandler);
    }

    private final BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Reset AllApps to its initial state only if we are not in the middle of
            // processing a multi-step drop
            if (mPendingRequestArgs == null) {
                mStateManager.goToState(NORMAL);
            }
        }
    };

    public void updateIconBadges(final Set<PackageUserKey> updatedBadges) {
        mWorkspace.updateIconBadges(updatedBadges);
        mAppsView.getAppsStore().updateIconBadges(updatedBadges);

        PopupContainerWithArrow popup = PopupContainerWithArrow.getOpen(Launcher.this);
        if (popup != null) {
            popup.updateNotificationHeader(updatedBadges);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        FirstFrameAnimatorHelper.initializeDrawListener(getWindow().getDecorView());
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onAttachedToWindow();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onDetachedFromWindow();
        }
    }

    public AllAppsTransitionController getAllAppsController() {
        return mAllAppsController;
    }

    @Override
    public LauncherRootView getRootView() {
        return (LauncherRootView) mLauncherView;
    }

    @Override
    public DragLayer getDragLayer() {
        return mDragLayer;
    }

    public AllAppsContainerView getAppsView() {
        return mAppsView;
    }

    public Workspace getWorkspace() {
        return mWorkspace;
    }

    public Hotseat getHotseat() {
        return mHotseat;
    }

    public View getHotseatSearchBox() {
        return mHotseatSearchBox;
    }

    public <T extends View> T getOverviewPanel() {
        return (T) mOverviewPanel;
    }

    public DropTargetBar getDropTargetBar() {
        return mDropTargetBar;
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    public ModelWriter getModelWriter() {
        return mModelWriter;
    }

    public SharedPreferences getSharedPrefs() {
        return mSharedPrefs;
    }

    public int getOrientation() { return mOldConfig.orientation; }

    @Override
    protected void onNewIntent(Intent intent) {
        TraceHelper.beginSection("NEW_INTENT");
        super.onNewIntent(intent);

        boolean alreadyOnHome = hasWindowFocus() && ((intent.getFlags() &
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

        // Check this condition before handling isActionMain, as this will get reset.
        boolean shouldMoveToDefaultScreen = alreadyOnHome && isInState(NORMAL)
                && AbstractFloatingView.getTopOpenView(this) == null;
        boolean isActionMain = Intent.ACTION_MAIN.equals(intent.getAction());
        boolean internalStateHandled = InternalStateHandler
                .handleNewIntent(this, intent, isStarted());

        if (isActionMain) {
            if (!internalStateHandled) {
                // Note: There should be at most one log per method call. This is enforced
                // implicitly by using if-else statements.
                UserEventDispatcher ued = getUserEventDispatcher();
                //liuzuo:add for addIcon:begin
                if(mEditFolderIcon!=null&&getImportMode()) {
                    forceExitImportMode();
                    return;
                }else {
                    closeFolder(true);
                }
                //liuzuo:add for addIcon:end

                //liuzuo:add the background of workspace when opening folder:begin
                if(mWorkspaceBgOpenFolder !=null&&mAniWorkspaceBg==null&& mWorkspaceBgOpenFolder.getVisibility()==View.VISIBLE){
                    openOrCloseFolderAnimation(false,mWorkspaceBgOpenFolder);
                }
                //liuzuo:add the background of workspace when opening folder:end
                AbstractFloatingView topOpenView = AbstractFloatingView.getTopOpenView(this);
                if (topOpenView != null) {
                    topOpenView.logActionCommand(Action.Command.HOME_INTENT);
                } else if (alreadyOnHome) {
                    Target target = newContainerTarget(mStateManager.getState().containerType);
                    target.pageIndex = mWorkspace.getCurrentPage();
                    ued.logActionCommand(Action.Command.HOME_INTENT, target,
                            newContainerTarget(ContainerType.WORKSPACE));
                }

                // In all these cases, only animate if we're already on home
                AbstractFloatingView.closeAllOpenViews(this, isStarted());

                if (!isInState(NORMAL)) {
                    // Only change state, if not already the same. This prevents cancelling any
                    // animations running as part of resume
                    mStateManager.goToState(NORMAL);
                }

                // Reset the apps view
                if (!alreadyOnHome) {
                    mAppsView.reset(isStarted() /* animate */);
                }

                if (shouldMoveToDefaultScreen && !mWorkspace.isTouchActive()) {
                    mWorkspace.post(mWorkspace::moveToDefaultScreen);
                }
            }

            final View v = getWindow().peekDecorView();
            if (v != null && v.getWindowToken() != null) {
                UiThreadHelper.hideKeyboardAsync(this, v.getWindowToken());
            }

            if (mLauncherCallbacks != null) {
                mLauncherCallbacks.onHomeIntent(internalStateHandled);
            }
        }

        TraceHelper.endSection("NEW_INTENT");
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mWorkspace.restoreInstanceStateForChild(mSynchronouslyBoundPage);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mWorkspace.getChildCount() > 0) {
            outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, mWorkspace.getNextPage());

        }
        outState.putInt(RUNTIME_STATE, mStateManager.getState().ordinal);


        AbstractFloatingView widgets = AbstractFloatingView
                .getOpenView(this, AbstractFloatingView.TYPE_WIDGETS_FULL_SHEET);
        if (widgets != null) {
            SparseArray<Parcelable> widgetsState = new SparseArray<>();
            widgets.saveHierarchyState(widgetsState);
            outState.putSparseParcelableArray(RUNTIME_STATE_WIDGET_PANEL, widgetsState);
        } else {
            outState.remove(RUNTIME_STATE_WIDGET_PANEL);
        }

        // We close any open folders and shortcut containers since they will not be re-opened,
        // and we need to make sure this state is reflected.
        AbstractFloatingView.closeAllOpenViews(this, false);

        if (mPendingRequestArgs != null) {
            outState.putParcelable(RUNTIME_STATE_PENDING_REQUEST_ARGS, mPendingRequestArgs);
        }
        if (mPendingActivityResult != null) {
            outState.putParcelable(RUNTIME_STATE_PENDING_ACTIVITY_RESULT, mPendingActivityResult);
        }

        super.onSaveInstanceState(outState);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mScreenOffReceiver);
        if(FeatureFlags.UNREAD_MESSAGE){
            UnreadInfoManager.getInstance(this).releaseRes();
        }
        mWorkspace.removeFolderListeners();

        UiFactory.setOnTouchControllersChangedListener(this, null);

        // Stop callbacks from LauncherModel
        // It's possible to receive onDestroy after a new Launcher activity has
        // been created. In this case, don't interfere with the new Launcher.
        //liuzuo:add the background of workspace when opening folder:begin
        WallpaperColorInfo.getInstance(this).setOnThemeChangeListener(null);
        //liuzuo:add the background of workspace when opening folder:end
        if (mModel.isCurrentCallbacks(this)) {
            mModel.stopLoader();
            LauncherAppState.getInstance(this).setLauncher(null);
        }
        mRotationHelper.destroy();

        try {
            mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            Log.w(TAG, "problem while stopping AppWidgetHost during Launcher destruction", ex);
        }

        TextKeyListener.getInstance().release();

        LauncherAnimUtils.onDestroyActivity();

        clearPendingBinds();

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onDestroy();
        }
    }

    public LauncherAccessibilityDelegate getAccessibilityDelegate() {
        return mAccessibilityDelegate;
    }

    public DragController getDragController() {
        return mDragController;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        super.startActivityForResult(intent, requestCode, options);
    }

    @Override
    public void startIntentSenderForResult (IntentSender intent, int requestCode,
            Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) {
        try {
            super.startIntentSenderForResult(intent, requestCode,
                fillInIntent, flagsMask, flagsValues, extraFlags, options);
        } catch (IntentSender.SendIntentException e) {
            throw new ActivityNotFoundException();
        }
    }

    /**
     * Indicates that we want global search for this activity by setting the globalSearch
     * argument for {@link #startSearch} to true.
     */
    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery,
            Bundle appSearchData, boolean globalSearch) {
        if (appSearchData == null) {
            appSearchData = new Bundle();
            appSearchData.putString("source", "launcher-search");
        }

        if (mLauncherCallbacks == null ||
                !mLauncherCallbacks.startSearch(initialQuery, selectInitialQuery, appSearchData)) {
            // Starting search from the callbacks failed. Start the default global search.
            super.startSearch(initialQuery, selectInitialQuery, appSearchData, true);
        }

        // We need to show the workspace after starting the search
        mStateManager.goToState(NORMAL);
    }

    public boolean isWorkspaceLocked() {
        return mWorkspaceLoading || mPendingRequestArgs != null;
    }

    public boolean isWorkspaceLoading() {
        return mWorkspaceLoading;
    }

    private void setWorkspaceLoading(boolean value) {
        mWorkspaceLoading = value;
    }

    public void setWaitingForResult(PendingRequestArgs args) {
        mPendingRequestArgs = args;
    }

    void addAppWidgetFromDropImpl(int appWidgetId, ItemInfo info, AppWidgetHostView boundWidget,
            WidgetAddFlowHandler addFlowHandler) {
        if (LOGD) {
            Log.d(TAG, "Adding widget from drop");
        }
        addAppWidgetImpl(appWidgetId, info, boundWidget, addFlowHandler, 0);
    }

    void addAppWidgetImpl(int appWidgetId, ItemInfo info,
            AppWidgetHostView boundWidget, WidgetAddFlowHandler addFlowHandler, int delay) {
        if (!addFlowHandler.startConfigActivity(this, appWidgetId, info, REQUEST_CREATE_APPWIDGET)) {
            // If the configuration flow was not started, add the widget

            Runnable onComplete = new Runnable() {
                @Override
                public void run() {
                    // Exit spring loaded mode if necessary after adding the widget
                    // Perry: Add edit mode panel: start
                    mStateManager.goToStateFrom(NORMAL, SPRING_LOADED, SPRING_LOADED_EXIT_DELAY);
                    // Perry: Add edit mode panel: end
                }
            };
            completeAddAppWidget(appWidgetId, info, boundWidget, addFlowHandler.getProviderInfo(this));
            mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete, delay, false);
        }
    }

    public void addPendingItem(PendingAddItemInfo info, long container, long screenId,
            int[] cell, int spanX, int spanY) {
        info.container = container;
        info.screenId = screenId;
        if (cell != null) {
            info.cellX = cell[0];
            info.cellY = cell[1];
        }
        info.spanX = spanX;
        info.spanY = spanY;

        switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET:
            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                addAppWidgetFromDrop((PendingAddWidgetInfo) info);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                processShortcutFromDrop((PendingAddShortcutInfo) info);
                break;
            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
            }
    }

    /**
     * Process a shortcut drop.
     */
    private void processShortcutFromDrop(PendingAddShortcutInfo info) {
        Intent intent = new Intent(Intent.ACTION_CREATE_SHORTCUT).setComponent(info.componentName);
        setWaitingForResult(PendingRequestArgs.forIntent(REQUEST_CREATE_SHORTCUT, intent, info));
        if (!info.activityInfo.startConfigActivity(this, REQUEST_CREATE_SHORTCUT)) {
            handleActivityResult(REQUEST_CREATE_SHORTCUT, RESULT_CANCELED, null);
        }
    }

    /**
     * Process a widget drop.
     */
    private void addAppWidgetFromDrop(PendingAddWidgetInfo info) {
        AppWidgetHostView hostView = info.boundWidget;
        final int appWidgetId;
        WidgetAddFlowHandler addFlowHandler = info.getHandler();
        if (hostView != null) {
            // In the case where we've prebound the widget, we remove it from the DragLayer
            if (LOGD) {
                Log.d(TAG, "Removing widget view from drag layer and setting boundWidget to null");
            }
            getDragLayer().removeView(hostView);

            appWidgetId = hostView.getAppWidgetId();
            addAppWidgetFromDropImpl(appWidgetId, info, hostView, addFlowHandler);

            // Clear the boundWidget so that it doesn't get destroyed.
            info.boundWidget = null;
        } else {
            // In this case, we either need to start an activity to get permission to bind
            // the widget, or we need to start an activity to configure the widget, or both.
            if (FeatureFlags.ENABLE_CUSTOM_WIDGETS &&
                    info.itemType == LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET) {
                appWidgetId = CustomWidgetParser.getWidgetIdForCustomProvider(
                        this, info.componentName);
            } else {
                appWidgetId = getAppWidgetHost().allocateAppWidgetId();
            }
            Bundle options = info.bindOptions;

            boolean success = mAppWidgetManager.bindAppWidgetIdIfAllowed(
                    appWidgetId, info.info, options);
            if (success) {
                addAppWidgetFromDropImpl(appWidgetId, info, null, addFlowHandler);
            } else {
                addFlowHandler.startBindFlow(this, appWidgetId, info, REQUEST_BIND_APPWIDGET);
            }
        }
    }

    FolderIcon addFolder(CellLayout layout, long container, final long screenId, int cellX,
            int cellY) {
        final FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = getText(R.string.folder_name);
        // Update the model
        getModelWriter().addItemToDatabase(folderInfo, container, screenId, cellX, cellY);

        // Create the view
        FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this, layout, folderInfo);
        mWorkspace.addInScreen(newFolder, folderInfo);
        // Force measure the new folder icon
        CellLayout parent = mWorkspace.getParentCellLayoutForView(newFolder);
        parent.getShortcutsAndWidgets().measureChild(newFolder);
        return newFolder;
    }

    /**
     * Unbinds the view for the specified item, and removes the item and all its children.
     *
     * @param v the view being removed.
     * @param itemInfo the {@link ItemInfo} for this view.
     * @param deleteFromDb whether or not to delete this item from the db.
     */
    public boolean removeItem(View v, final ItemInfo itemInfo, boolean deleteFromDb) {
        if (itemInfo instanceof ShortcutInfo) {
            // Remove the shortcut from the folder before removing it from launcher
            View folderIcon = mWorkspace.getHomescreenIconByItemId(itemInfo.container);
            if (folderIcon instanceof FolderIcon) {
                ((FolderInfo) folderIcon.getTag()).remove((ShortcutInfo) itemInfo, true);
            } else {
                mWorkspace.removeWorkspaceItem(v);
            }
            if (deleteFromDb) {
                getModelWriter().deleteItemFromDatabase(itemInfo);
            }
        } else if (itemInfo instanceof FolderInfo) {
            final FolderInfo folderInfo = (FolderInfo) itemInfo;
            if (v instanceof FolderIcon) {
                ((FolderIcon) v).removeListeners();
            }
            mWorkspace.removeWorkspaceItem(v);
            if (deleteFromDb) {
                getModelWriter().deleteFolderAndContentsFromDatabase(folderInfo);
            }
        } else if (itemInfo instanceof LauncherAppWidgetInfo) {
            final LauncherAppWidgetInfo widgetInfo = (LauncherAppWidgetInfo) itemInfo;
            mWorkspace.removeWorkspaceItem(v);
            if (deleteFromDb) {
                deleteWidgetInfo(widgetInfo);
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Deletes the widget info and the widget id.
     */
    private void deleteWidgetInfo(final LauncherAppWidgetInfo widgetInfo) {
        final LauncherAppWidgetHost appWidgetHost = getAppWidgetHost();
        if (appWidgetHost != null && !widgetInfo.isCustomWidget() && widgetInfo.isWidgetIdAllocated()) {
            // Deleting an app widget ID is a void call but writes to disk before returning
            // to the caller...
            new AsyncTask<Void, Void, Void>() {
                public Void doInBackground(Void ... args) {
                    appWidgetHost.deleteAppWidgetId(widgetInfo.appWidgetId);
                    return null;
                }
            }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR);
        }
        getModelWriter().deleteItemFromDatabase(widgetInfo);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return (event.getKeyCode() == KeyEvent.KEYCODE_HOME) || super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (finishAutoCancelActionMode()) {
            return;
        }
        if (mLauncherCallbacks != null && mLauncherCallbacks.handleBackPressed()) {
            return;
        }

        if (mDragController.isDragging()) {
            mDragController.cancelDrag();
            return;
        }

        // Note: There should be at most one log per method call. This is enforced implicitly
        // by using if-else statements.
        UserEventDispatcher ued = getUserEventDispatcher();
        AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(this);
        if (topView != null && topView.onBackPressed()) {
            // Handled by the floating view.
        } else if (!isInState(NORMAL)) {
            LauncherState lastState = mStateManager.getLastState();
            ued.logActionCommand(Action.Command.BACK, mStateManager.getState().containerType,
                    lastState.containerType);
            if (isInState(EDIT_MODE)) {
                if (!getEditModePanel().backToNormalPanel()) {
                    mStateManager.goToState(lastState);
                }
            }
            //liuzuo : add for addIcon : start
            else if (Folder.getOpen(this) != null) {
                Folder openFolder = Folder.getOpen(this);
                if (openFolder.isEditingName()) {
                    openFolder.dismissEditingName();
                } else {
                    closeFolder(true);
                }
            }else if(mEditFolderIcon!=null&&getImportMode()) {
                if (mEditFolderIcon.getFolder() != null && mEditFolderIcon.getFolder().isAnimating()) {
                    return;
                }
                showFolderIcon();
                mOpenFolder = true;
                if (mCheckedBubbleTextViews != null) {
                    for (BubbleTextView bv : mCheckedBubbleTextViews
                            ) {
                        bv.setChecked(false);
                    }
                }
                mEditFolderIcon.getFolder().setImportMode(false);
                mCheckedBubbleTextViews.clear();
                mCheckedShortcutInfos.clear();
                exitEditModeAndOpenFolder();
            }
            //liuzuo : add for addIcon : end
            else {
                mStateManager.goToState(lastState);
            }
        }else {
            // Back button is a no-op here, but give at least some feedback for the button press
            //liuzuo:add the background of workspace when opening folder:begin
            if(mWorkspaceBgOpenFolder !=null&& mWorkspaceBgOpenFolder.getVisibility()==View.VISIBLE){
                openOrCloseFolderAnimation(false,mWorkspaceBgOpenFolder);
            }
            //liuzuo:add the background of workspace when opening folder:end
            mWorkspace.showOutlinesTemporarily();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public ActivityOptions getActivityLaunchOptions(View v) {
        return mAppTransitionManager.getActivityLaunchOptions(this, v);
    }

    public LauncherAppTransitionManager getAppTransitionManager() {
        return mAppTransitionManager;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected boolean onErrorStartingShortcut(Intent intent, ItemInfo info) {
        // Due to legacy reasons, direct call shortcuts require Launchers to have the
        // corresponding permission. Show the appropriate permission prompt if that
        // is the case.
        if (intent.getComponent() == null
                && Intent.ACTION_CALL.equals(intent.getAction())
                && checkSelfPermission(android.Manifest.permission.CALL_PHONE) !=
                PackageManager.PERMISSION_GRANTED) {

            setWaitingForResult(PendingRequestArgs
                    .forIntent(REQUEST_PERMISSION_CALL_PHONE, intent, info));
            requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE},
                    REQUEST_PERMISSION_CALL_PHONE);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void modifyUserEvent(LauncherLogProto.LauncherEvent event) {
        if (event.srcTarget != null && event.srcTarget.length > 0 &&
                event.srcTarget[1].containerType == ContainerType.PREDICTION) {
            Target[] targets = new Target[3];
            targets[0] = event.srcTarget[0];
            targets[1] = event.srcTarget[1];
            targets[2] = newTarget(Target.Type.CONTAINER);
            event.srcTarget = targets;
            LauncherState state = mStateManager.getState();
            if (state == LauncherState.ALL_APPS) {
                event.srcTarget[2].containerType = ContainerType.ALLAPPS;
            } else if (state == LauncherState.OVERVIEW) {
                event.srcTarget[2].containerType = ContainerType.TASKSWITCHER;
            }
        }
    }

    public boolean startActivitySafely(View v, Intent intent, ItemInfo item) {
        boolean success = super.startActivitySafely(v, intent, item);
        if (success && v instanceof BubbleTextView) {
            // This is set to the view that launched the activity that navigated the user away
            // from launcher. Since there is no callback for when the activity has finished
            // launching, enable the press state and keep this reference to reset the press
            // state when we return to launcher.
            BubbleTextView btv = (BubbleTextView) v;
            btv.setStayPressed(true);
            setOnResumeCallback(btv);
        }
        return success;
    }

    boolean isHotseatLayout(View layout) {
        // TODO: Remove this method
        return mHotseat != null && layout != null &&
                (layout instanceof CellLayout) && (layout == mHotseat.getLayout());
    }

    /**
     * Returns the CellLayout of the specified container at the specified screen.
     */
    public CellLayout getCellLayout(long container, long screenId) {
        if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            if (mHotseat != null) {
                return mHotseat.getLayout();
            } else {
                return null;
            }
        } else {
            return mWorkspace.getScreenWithId(screenId);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // The widget preview db can result in holding onto over
            // 3MB of memory for caching which isn't necessary.
            SQLiteDatabase.releaseMemory();

            // This clears all widget bitmaps from the widget tray
            // TODO(hyunyoungs)
        }
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onTrimMemory(level);
        }
        UiFactory.onTrimMemory(this, level);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        final boolean result = super.dispatchPopulateAccessibilityEvent(event);
        final List<CharSequence> text = event.getText();
        text.clear();
        // Populate event with a fake title based on the current state.
        // TODO: When can workspace be null?
        text.add(mWorkspace == null
                ? getString(R.string.all_apps_home_button_label)
                : mStateManager.getState().getDescription(this));
        return result;
    }

    public void setOnResumeCallback(OnResumeCallback callback) {
        if (mOnResumeCallback != null) {
            mOnResumeCallback.onLauncherResume();
        }
        mOnResumeCallback = callback;
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public int getCurrentWorkspaceScreen() {
        if (mWorkspace != null) {
            return mWorkspace.getCurrentPage();
        } else {
            return 0;
        }
    }

    /**
     * Clear any pending bind callbacks. This is called when is loader is planning to
     * perform a full rebind from scratch.
     */
    @Override
    public void clearPendingBinds() {
        if (mPendingExecutor != null) {
            mPendingExecutor.markCompleted();
            mPendingExecutor = null;
        }
    }

    /**
     * Refreshes the shortcuts shown on the workspace.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void startBinding() {
        TraceHelper.beginSection("startBinding");
        // Floating panels (except the full widget sheet) are associated with individual icons. If
        // we are starting a fresh bind, close all such panels as all the icons are about
        // to go away.
        AbstractFloatingView.closeOpenViews(this, true,
                AbstractFloatingView.TYPE_ALL & ~AbstractFloatingView.TYPE_REBIND_SAFE);

        setWorkspaceLoading(true);

        // Clear the workspace because it's going to be rebound
        mWorkspace.clearDropTargets();
        mWorkspace.removeAllWorkspaceScreens();
        mAppWidgetHost.clearViews();

        if (mHotseat != null) {
            mHotseat.resetLayout(mDeviceProfile.isVerticalBarLayout());
        }
        TraceHelper.endSection("startBinding");
    }

    @Override
    public void bindScreens(ArrayList<Long> orderedScreenIds) {
        // Make sure the first screen is always at the start.
        if (FeatureFlags.QSB_ON_FIRST_SCREEN &&
                orderedScreenIds.indexOf(Workspace.FIRST_SCREEN_ID) != 0) {
            orderedScreenIds.remove(Workspace.FIRST_SCREEN_ID);
            orderedScreenIds.add(0, Workspace.FIRST_SCREEN_ID);
            LauncherModel.updateWorkspaceScreenOrder(this, orderedScreenIds);
        } else if (!FeatureFlags.QSB_ON_FIRST_SCREEN && orderedScreenIds.isEmpty()) {
            // If there are no screens, we need to have an empty screen
            mWorkspace.addExtraEmptyScreen();
        }
        bindAddScreens(orderedScreenIds);

        // After we have added all the screens, if the wallpaper was locked to the default state,
        // then notify to indicate that it can be released and a proper wallpaper offset can be
        // computed before the next layout
        mWorkspace.unlockWallpaperFromDefaultPageOnNextLayout();
    }

    private void bindAddScreens(ArrayList<Long> orderedScreenIds) {
        int count = orderedScreenIds.size();
        for (int i = 0; i < count; i++) {
            long screenId = orderedScreenIds.get(i);
            if (!FeatureFlags.QSB_ON_FIRST_SCREEN || screenId != Workspace.FIRST_SCREEN_ID) {
                // No need to bind the first screen, as its always bound.
                mWorkspace.insertNewWorkspaceScreenBeforeEmptyScreen(screenId);
            }
        }
    }

    @Override
    public void bindAppsAdded(ArrayList<Long> newScreens, ArrayList<ItemInfo> addNotAnimated,
            ArrayList<ItemInfo> addAnimated) {
        // Add the new screens
        if (newScreens != null) {
            bindAddScreens(newScreens);
        }

        // We add the items without animation on non-visible pages, and with
        // animations on the new page (which we will try and snap to).
        if (addNotAnimated != null && !addNotAnimated.isEmpty()) {
            bindItems(addNotAnimated, false);
        }
        if (addAnimated != null && !addAnimated.isEmpty()) {
            bindItems(addAnimated, true);
        }

        // Remove the extra empty screen
        mWorkspace.removeExtraEmptyScreen(false, false);
    }

    /**
     * Bind the items start-end from the list.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindItems(final List<ItemInfo> items, final boolean forceAnimateIcons) {
        // Get the list of added items and intersect them with the set of items here
        final AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        final Collection<Animator> bounceAnims = new ArrayList<>();
        // Perry: Resolve folder's release error: start
        final boolean animateIcons = forceAnimateIcons/* && canRunNewAppsAnimation()*/;
        // Perry: Resolve folder's release error: end
        Workspace workspace = mWorkspace;
        long newItemsScreenId = -1;
        int end = items.size();
        for (int i = 0; i < end; i++) {
            final ItemInfo item = items.get(i);

            // Short circuit if we are loading dock items for a configuration which has no dock
            if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT &&
                    mHotseat == null) {
                continue;
            }

            final View view;
            switch (item.itemType) {
                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT: {
                    ShortcutInfo info = (ShortcutInfo) item;
                    view = createShortcut(info);
                    break;
                }
                case LauncherSettings.Favorites.ITEM_TYPE_FOLDER: {
                    view = FolderIcon.fromXml(R.layout.folder_icon, this,
                            (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()),
                            (FolderInfo) item);
                    break;
                }
                case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                case LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET: {
                    view = inflateAppWidget((LauncherAppWidgetInfo) item);
                    if (view == null) {
                        continue;
                    }
                    break;
                }
                default:
                    throw new RuntimeException("Invalid Item Type");
            }

             /*
             * Remove colliding items.
             */
            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                CellLayout cl = mWorkspace.getScreenWithId(item.screenId);
                if (cl != null && cl.isOccupied(item.cellX, item.cellY)) {
                    View v = cl.getChildAt(item.cellX, item.cellY);
                    Object tag = v.getTag();
                    String desc = "Collision while binding workspace item: " + item
                            + ". Collides with " + tag;
                    if (FeatureFlags.IS_DOGFOOD_BUILD) {
                        throw (new RuntimeException(desc));
                    } else {
                        Log.d(TAG, desc);
                        getModelWriter().deleteItemFromDatabase(item);
                        continue;
                    }
                }
            }
            workspace.addInScreenFromBind(view, item);
            if (animateIcons) {
                // Animate all the applications up now
                view.setAlpha(0f);
                view.setScaleX(0f);
                view.setScaleY(0f);
                bounceAnims.add(createNewAppBounceAnimation(view, i));
                newItemsScreenId = item.screenId;
            }
        }

        if (animateIcons) {
            // Animate to the correct page
            if (newItemsScreenId > -1) {
                long currentScreenId = mWorkspace.getScreenIdForPageIndex(mWorkspace.getNextPage());
                final int newScreenIndex = mWorkspace.getPageIndexForScreenId(newItemsScreenId);
                final Runnable startBounceAnimRunnable = new Runnable() {
                    public void run() {
                        anim.playTogether(bounceAnims);
                        anim.start();
                    }
                };
                if (newItemsScreenId != currentScreenId) {
                    // We post the animation slightly delayed to prevent slowdowns
                    // when we are loading right after we return to launcher.
                    mWorkspace.postDelayed(new Runnable() {
                        public void run() {
                            if (mWorkspace != null) {
                                AbstractFloatingView.closeAllOpenViews(Launcher.this, false);

                                mWorkspace.snapToPage(newScreenIndex);
                                mWorkspace.postDelayed(startBounceAnimRunnable,
                                        NEW_APPS_ANIMATION_DELAY);
                            }
                        }
                    }, NEW_APPS_PAGE_MOVE_DELAY);
                } else {
                    mWorkspace.postDelayed(startBounceAnimRunnable, NEW_APPS_ANIMATION_DELAY);
                }
            }
        }
        workspace.requestLayout();
    }

    /**
     * Add the views for a widget to the workspace.
     */
    public void bindAppWidget(LauncherAppWidgetInfo item) {
        View view = inflateAppWidget(item);
        if (view != null) {
            mWorkspace.addInScreen(view, item);
            mWorkspace.requestLayout();
        }
    }

    private View inflateAppWidget(LauncherAppWidgetInfo item) {
        if (mIsSafeModeEnabled) {
            PendingAppWidgetHostView view =
                    new PendingAppWidgetHostView(this, item, mIconCache, true);
            prepareAppWidget(view, item);
            return view;
        }

        TraceHelper.beginSection("BIND_WIDGET");

        final LauncherAppWidgetProviderInfo appWidgetInfo;

        if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY)) {
            // If the provider is not ready, bind as a pending widget.
            appWidgetInfo = null;
        } else if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_NOT_VALID)) {
            // The widget id is not valid. Try to find the widget based on the provider info.
            appWidgetInfo = mAppWidgetManager.findProvider(item.providerName, item.user);
        } else {
            appWidgetInfo = mAppWidgetManager.getLauncherAppWidgetInfo(item.appWidgetId);
        }

        // If the provider is ready, but the width is not yet restored, try to restore it.
        if (!item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY) &&
                (item.restoreStatus != LauncherAppWidgetInfo.RESTORE_COMPLETED)) {
            if (appWidgetInfo == null) {
                Log.d(TAG, "Removing restored widget: id=" + item.appWidgetId
                        + " belongs to component " + item.providerName
                        + ", as the provider is null");
                getModelWriter().deleteItemFromDatabase(item);
                return null;
            }

            // If we do not have a valid id, try to bind an id.
            if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_NOT_VALID)) {
                if (!item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_ALLOCATED)) {
                    // Id has not been allocated yet. Allocate a new id.
                    item.appWidgetId = mAppWidgetHost.allocateAppWidgetId();
                    item.restoreStatus |= LauncherAppWidgetInfo.FLAG_ID_ALLOCATED;

                    // Also try to bind the widget. If the bind fails, the user will be shown
                    // a click to setup UI, which will ask for the bind permission.
                    PendingAddWidgetInfo pendingInfo = new PendingAddWidgetInfo(appWidgetInfo);
                    pendingInfo.spanX = item.spanX;
                    pendingInfo.spanY = item.spanY;
                    pendingInfo.minSpanX = item.minSpanX;
                    pendingInfo.minSpanY = item.minSpanY;
                    Bundle options = WidgetHostViewLoader.getDefaultOptionsForWidget(this, pendingInfo);

                    boolean isDirectConfig =
                            item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_DIRECT_CONFIG);
                    if (isDirectConfig && item.bindOptions != null) {
                        Bundle newOptions = item.bindOptions.getExtras();
                        if (options != null) {
                            newOptions.putAll(options);
                        }
                        options = newOptions;
                    }
                    boolean success = mAppWidgetManager.bindAppWidgetIdIfAllowed(
                            item.appWidgetId, appWidgetInfo, options);

                    // We tried to bind once. If we were not able to bind, we would need to
                    // go through the permission dialog, which means we cannot skip the config
                    // activity.
                    item.bindOptions = null;
                    item.restoreStatus &= ~LauncherAppWidgetInfo.FLAG_DIRECT_CONFIG;

                    // Bind succeeded
                    if (success) {
                        // If the widget has a configure activity, it is still needs to set it up,
                        // otherwise the widget is ready to go.
                        item.restoreStatus = (appWidgetInfo.configure == null) || isDirectConfig
                                ? LauncherAppWidgetInfo.RESTORE_COMPLETED
                                : LauncherAppWidgetInfo.FLAG_UI_NOT_READY;
                    }

                    getModelWriter().updateItemInDatabase(item);
                }
            } else if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_UI_NOT_READY)
                    && (appWidgetInfo.configure == null)) {
                // The widget was marked as UI not ready, but there is no configure activity to
                // update the UI.
                item.restoreStatus = LauncherAppWidgetInfo.RESTORE_COMPLETED;
                getModelWriter().updateItemInDatabase(item);
            }
        }

        final AppWidgetHostView view;
        if (item.restoreStatus == LauncherAppWidgetInfo.RESTORE_COMPLETED) {
            // Verify that we own the widget
            if (appWidgetInfo == null) {
                FileLog.e(TAG, "Removing invalid widget: id=" + item.appWidgetId);
                deleteWidgetInfo(item);
                return null;
            }

            item.minSpanX = appWidgetInfo.minSpanX;
            item.minSpanY = appWidgetInfo.minSpanY;
            view = mAppWidgetHost.createView(this, item.appWidgetId, appWidgetInfo);
        } else {
            view = new PendingAppWidgetHostView(this, item, mIconCache, false);
        }
        prepareAppWidget(view, item);

        TraceHelper.endSection("BIND_WIDGET", "id=" + item.appWidgetId);
        return view;
    }

    /**
     * Restores a pending widget.
     *
     * @param appWidgetId The app widget id
     */
    private LauncherAppWidgetInfo completeRestoreAppWidget(int appWidgetId, int finalRestoreFlag) {
        LauncherAppWidgetHostView view = mWorkspace.getWidgetForAppWidgetId(appWidgetId);
        if ((view == null) || !(view instanceof PendingAppWidgetHostView)) {
            Log.e(TAG, "Widget update called, when the widget no longer exists.");
            return null;
        }

        LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) view.getTag();
        info.restoreStatus = finalRestoreFlag;
        if (info.restoreStatus == LauncherAppWidgetInfo.RESTORE_COMPLETED) {
            info.pendingItemInfo = null;
        }

        if (((PendingAppWidgetHostView) view).isReinflateIfNeeded()) {
            view.reInflate();
        }

        getModelWriter().updateItemInDatabase(info);
        return info;
    }

    public void onPageBoundSynchronously(int page) {
        mSynchronouslyBoundPage = page;
    }

    @Override
    public void executeOnNextDraw(ViewOnDrawExecutor executor) {
        if (mPendingExecutor != null) {
            mPendingExecutor.markCompleted();
        }
        mPendingExecutor = executor;
        if (!isInState(ALL_APPS)) {
            mAppsView.getAppsStore().setDeferUpdates(true);
            mPendingExecutor.execute(() -> mAppsView.getAppsStore().setDeferUpdates(false));
        }

        executor.attachTo(this);
    }

    public void clearPendingExecutor(ViewOnDrawExecutor executor) {
        if (mPendingExecutor == executor) {
            mPendingExecutor = null;
        }
    }

    @Override
    public void finishFirstPageBind(final ViewOnDrawExecutor executor) {
        AlphaProperty property = mDragLayer.getAlphaProperty(ALPHA_INDEX_LAUNCHER_LOAD);
        if (property.getValue() < 1) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(property, MultiValueAlpha.VALUE, 1);
            if (executor != null) {
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        executor.onLoadAnimationCompleted();
                    }
                });
            }
            anim.start();
        } else if (executor != null) {
            executor.onLoadAnimationCompleted();
        }
    }

    /**
     * Callback saying that there aren't any more items to bind.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void finishBindingItems() {
        TraceHelper.beginSection("finishBindingItems");
        mWorkspace.restoreInstanceStateForRemainingPages();

        setWorkspaceLoading(false);

        if (mPendingActivityResult != null) {
            handleActivityResult(mPendingActivityResult.requestCode,
                    mPendingActivityResult.resultCode, mPendingActivityResult.data);
            mPendingActivityResult = null;
        }

        // Perry: Hotseat reorder function: start
        mHotseat.arrangeContent();
        // Perry: Hotseat reorder function: end

        InstallShortcutReceiver.disableAndFlushInstallQueue(
                InstallShortcutReceiver.FLAG_LOADER_RUNNING, this);
        //Bruce : add for unread message : start
        if (FeatureFlags.UNREAD_MESSAGE) {
            initUnreadMessage();
        }
        //Bruce : add for unread message : end
        TraceHelper.endSection("finishBindingItems");
    }

    private boolean canRunNewAppsAnimation() {
        long diff = System.currentTimeMillis() - mDragController.getLastGestureUpTime();
        return diff > (NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS * 1000);
    }

    protected ValueAnimator createNewAppBounceAnimation(View v, int i) {
        ValueAnimator bounceAnim = LauncherAnimUtils.ofViewAlphaAndScale(v, 1, 1, 1);
        bounceAnim.setDuration(InstallShortcutReceiver.NEW_SHORTCUT_BOUNCE_DURATION);
        bounceAnim.setStartDelay(i * InstallShortcutReceiver.NEW_SHORTCUT_STAGGER_DELAY);
        bounceAnim.setInterpolator(new OvershootInterpolator(BOUNCE_ANIMATION_TENSION));
        return bounceAnim;
    }

    /**
     * Add the icons for all apps.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAllApplications(ArrayList<AppInfo> apps) {
        mAppsView.getAppsStore().setApps(apps);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.bindAllApplications(apps);
        }
    }

    /**
     * Copies LauncherModel's map of activities to shortcut ids to Launcher's. This is necessary
     * because LauncherModel's map is updated in the background, while Launcher runs on the UI.
     */
    @Override
    public void bindDeepShortcutMap(MultiHashMap<ComponentKey, String> deepShortcutMapCopy) {
        mPopupDataProvider.setDeepShortcutMap(deepShortcutMapCopy);
    }

    @Override
    public void relaunch() {
    }

    // Perry: Automatically fill vacancies: start
    @Override
    public void bindScreenItemsMoved(long screenId, LongSparseArray<ItemInfo> movedItems, boolean animate) {
    }
    // Perry: Automatically fill vacancies: end

    /**
     * A package was updated.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindAppsAddedOrUpdated(ArrayList<AppInfo> apps) {
        mAppsView.getAppsStore().addOrUpdateApps(apps);
    }

    @Override
    public void bindPromiseAppProgressUpdated(PromiseAppInfo app) {
        mAppsView.getAppsStore().updatePromiseAppProgress(app);
    }

    @Override
    public void bindWidgetsRestored(ArrayList<LauncherAppWidgetInfo> widgets) {
        mWorkspace.widgetsRestored(widgets);
    }

    /**
     * Some shortcuts were updated in the background.
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @param updated list of shortcuts which have changed.
     */
    @Override
    public void bindShortcutsChanged(ArrayList<ShortcutInfo> updated, final UserHandle user) {
        if (!updated.isEmpty()) {
            mWorkspace.updateShortcuts(updated);
        }
    }

    /**
     * Update the state of a package, typically related to install state.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindRestoreItemsChange(HashSet<ItemInfo> updates) {
        mWorkspace.updateRestoreItems(updates);
    }

    /**
     * A package was uninstalled/updated.  We take both the super set of packageNames
     * in addition to specific applications to remove, the reason being that
     * this can be called when a package is updated as well.  In that scenario,
     * we only remove specific components from the workspace and hotseat, where as
     * package-removal should clear all items by package name.
     */
    @Override
    public void bindWorkspaceComponentsRemoved(final ItemInfoMatcher matcher) {
        mWorkspace.removeItemsByMatcher(matcher);
        mDragController.onAppsRemoved(matcher);
    }

    @Override
    public void bindAppInfosRemoved(final ArrayList<AppInfo> appInfos) {
        mAppsView.getAppsStore().removeApps(appInfos);
    }

    @Override
    public void bindAllWidgets(final ArrayList<WidgetListRowEntry> allWidgets) {
        mPopupDataProvider.setAllWidgets(allWidgets);
        AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(this);
        if (topView != null) {
            topView.onWidgetsBound();
        }
    }

    /**
     * @param packageUser if null, refreshes all widgets and shortcuts, otherwise only
     *                    refreshes the widgets and shortcuts associated with the given package/user
     */
    public void refreshAndBindWidgetsForPackageUser(@Nullable PackageUserKey packageUser) {
        mModel.refreshAndBindWidgetsAndShortcuts(packageUser);
    }

    /**
     * $ adb shell dumpsys activity com.android.launcher3.Launcher [--all]
     */
    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);

        if (args.length > 0 && TextUtils.equals(args[0], "--all")) {
            writer.println(prefix + "Workspace Items");
            for (int i = 0; i < mWorkspace.getPageCount(); i++) {
                writer.println(prefix + "  Homescreen " + i);

                ViewGroup layout = ((CellLayout) mWorkspace.getPageAt(i)).getShortcutsAndWidgets();
                for (int j = 0; j < layout.getChildCount(); j++) {
                    Object tag = layout.getChildAt(j).getTag();
                    if (tag != null) {
                        writer.println(prefix + "    " + tag.toString());
                    }
                }
            }

            writer.println(prefix + "  Hotseat");
            ViewGroup layout = mHotseat.getLayout().getShortcutsAndWidgets();
            for (int j = 0; j < layout.getChildCount(); j++) {
                Object tag = layout.getChildAt(j).getTag();
                if (tag != null) {
                    writer.println(prefix + "    " + tag.toString());
                }
            }
        }

        writer.println(prefix + "Misc:");
        writer.print(prefix + "\tmWorkspaceLoading=" + mWorkspaceLoading);
        writer.print(" mPendingRequestArgs=" + mPendingRequestArgs);
        writer.println(" mPendingActivityResult=" + mPendingActivityResult);
        writer.println(" mRotationHelper: " + mRotationHelper);
        dumpMisc(writer);

        try {
            FileLog.flushAll(writer);
        } catch (Exception e) {
            // Ignore
        }

        mModel.dumpState(prefix, fd, writer, args);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.dump(prefix, fd, writer, args);
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.N)
    public void onProvideKeyboardShortcuts(
            List<KeyboardShortcutGroup> data, Menu menu, int deviceId) {

        ArrayList<KeyboardShortcutInfo> shortcutInfos = new ArrayList<>();
        if (isInState(NORMAL)) {
            shortcutInfos.add(new KeyboardShortcutInfo(getString(R.string.all_apps_button_label),
                    KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON));
            shortcutInfos.add(new KeyboardShortcutInfo(getString(R.string.widget_button_text),
                    KeyEvent.KEYCODE_W, KeyEvent.META_CTRL_ON));
        }
        final View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            if (new CustomActionsPopup(this, currentFocus).canShow()) {
                shortcutInfos.add(new KeyboardShortcutInfo(getString(R.string.custom_actions),
                        KeyEvent.KEYCODE_O, KeyEvent.META_CTRL_ON));
            }
            if (currentFocus.getTag() instanceof ItemInfo
                    && DeepShortcutManager.supportsShortcuts((ItemInfo) currentFocus.getTag())) {
                shortcutInfos.add(new KeyboardShortcutInfo(
                        getString(R.string.shortcuts_menu_with_notifications_description),
                        KeyEvent.KEYCODE_S, KeyEvent.META_CTRL_ON));
            }
        }
        if (!shortcutInfos.isEmpty()) {
            data.add(new KeyboardShortcutGroup(getString(R.string.home_screen), shortcutInfos));
        }

        super.onProvideKeyboardShortcuts(data, menu, deviceId);
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        if (event.hasModifiers(KeyEvent.META_CTRL_ON)) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_A:
                    if (isInState(NORMAL)) {
                        getStateManager().goToState(ALL_APPS);
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_S: {
                    View focusedView = getCurrentFocus();
                    if (focusedView instanceof BubbleTextView
                            && focusedView.getTag() instanceof ItemInfo
                            && mAccessibilityDelegate.performAction(focusedView,
                                    (ItemInfo) focusedView.getTag(),
                                    LauncherAccessibilityDelegate.DEEP_SHORTCUTS)) {
                        PopupContainerWithArrow.getOpen(this).requestFocus();
                        return true;
                    }
                    break;
                }
                case KeyEvent.KEYCODE_O:
                    if (new CustomActionsPopup(this, getCurrentFocus()).show()) {
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_W:
                    if (isInState(NORMAL)) {
                        OptionsPopupView.openWidgets(this);
                        return true;
                    }
                    break;
            }
        }
        return super.onKeyShortcut(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            // KEYCODE_MENU is sent by some tests, for example
            // LauncherJankTests#testWidgetsContainerFling. Don't just remove its handling.
            if (!mDragController.isDragging() && !mWorkspace.isSwitchingState() &&
                    isInState(NORMAL)) {
                // Close any open floating views.
                AbstractFloatingView.closeAllOpenViews(this);

                // Setting the touch point to (-1, -1) will show the options popup in the center of
                // the screen.
                OptionsPopupView.showDefaultOptions(this, -1, -1);
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Called when the user starts interacting with the launcher.
     * The possible interactions are:
     *  - open all apps
     *  - reorder an app shortcut, or a widget
     *  - open the overview mode.
     * This is a good time to stop doing things that only make sense
     * when the user is on the homescreen and not doing housekeeping.
     */
    protected void onInteractionBegin() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onInteractionBegin();
        }
    }

    /**
     * Called when the user stops interacting with the launcher.
     * This implies that the user is now on the homescreen and is not doing housekeeping.
     */
    protected void onInteractionEnd() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onInteractionEnd();
        }
    }

    public static Launcher getLauncher(Context context) {
        if (context instanceof Launcher) {
            return (Launcher) context;
        }
        return ((Launcher) ((ContextWrapper) context).getBaseContext());
    }

    /**
     * Callback for listening for onResume
     */
    public interface OnResumeCallback {

        void onLauncherResume();
    }

    // Perry: desktop switch function: start
    public boolean minusOneEnable() {
        return mWorkspace.hasOverlay();
    }
    // Perry: desktop switch function: end

    //liuzuo:add the background of workspace when opening folder:begin
    private synchronized void setWallPaperBlur(Bitmap wallpaperThumb, boolean b) {
        Bitmap bm = null;
        Bitmap clipBlur = null ;
        Boolean isLiveWallpaper = false;
        if (wallpaperThumb != null) {
            bm = wallpaperThumb;
            Log.d(TAG, "setWallPaperBlur  Wallpaper ");
            if(b){
                isLiveWallpaper = true;
            }
        } else {
            Log.d(TAG, "setWallPaperBlur  Wallpaper == null");
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
            if (b) {
                List<WallpaperPicker.WallpaperTileInfo> wallpaperInfoList = getWallpaperInfoList();
                WallpaperInfo wallpaperInfo = wallpaperManager.getWallpaperInfo();
                if (wallpaperInfoList != null&&wallpaperInfo!=null) {
                    String packageName = wallpaperInfo.getServiceName();
                    for (WallpaperPicker.WallpaperTileInfo tileInfo : wallpaperInfoList) {
                        if (tileInfo instanceof WallpaperPicker.LiveWallpaperInfo &&packageName != null && packageName.equals(
                                ((WallpaperPicker.LiveWallpaperInfo) tileInfo).mInfo
                                        .getServiceName())) {
                            bm = ((BitmapDrawable) tileInfo.mThumb).getBitmap();
                            isLiveWallpaper = true;
                            Log.d(TAG, "setWallPaperBlur  liveWallpaper ");
                            break;
                        }
                    }
                }
            }
            Drawable d = null ;
            try {
                d = wallpaperManager.getDrawable();

            }catch (Exception e){
                d = getResources().getDrawable(R.drawable.wallpaper_05);
                Log.d(TAG,"setWallPaperBlur error"+e.toString());
            }
            if (!isLiveWallpaper&&d instanceof BitmapDrawable) {
                Log.d(TAG, "setWallPaperBlur from wallpaperManager");
                bm = ((BitmapDrawable) d).getBitmap();
                isLiveWallpaper = false;
            }
        }
        if(bm!=null){
            clipBlur = Bitmap.createBitmap(bm)/*clipBlur(bm,isLiveWallpaper)*/;
        }
        if(clipBlur==null||clipBlur.isRecycled()){
            return;
        }
        Bitmap oldBlur = null;
        if (mBlur != null && !mBlur.isRecycled()) {
            oldBlur = mBlur;
        }
        float scale;
        if (isLiveWallpaper) {
            scale = 0.5f;
        } else {
            scale = 0.1f;
        }
        if(clipBlur!=null&&!clipBlur.isRecycled()){
            mBlur = BlurFactory.blur(this, clipBlur, scale, 10);// radius 1~25
            if (mBlur != null&& mWorkspaceBgOpenFolder !=null){
                mWorkspaceBgOpenFolder.setImageBitmap(mBlur);

            }
           /* if (mBlur != null&& mWidgetsView !=null){
                mWidgetsView.setBackground(new FastBitmapDrawable(mBlur));
            }*/
            if(oldBlur!=null && !oldBlur.isRecycled()){
                oldBlur.recycle();
            }
            Log.d(TAG, "setWallPaperBlur  image scale = "+scale);
        }
    }

    /**
     * liuzuo add for get WallpaperInfos
     * @return WallpaperInfos
     */
    private List<WallpaperPicker.WallpaperTileInfo> getWallpaperInfoList(){
        if(mWallpaperPicker!=null){
            return mWallpaperPicker.getWallpaperInfoList();
        }else {
            return  null ;
        }
    }

    private Bitmap clipBlur(Bitmap bitmap, Boolean isLiveWallpaper) {
        if (bitmap != null) {
            DeviceProfile deviceProfile = getDeviceProfile();
            float ratio = (float) deviceProfile.widthPx / deviceProfile.heightPx;
            int height = bitmap.getHeight();
            int width = bitmap.getWidth();
            float bitmapRatio = (float) width / height;
            if (bitmapRatio > ratio) {
                int clipBitmapWidth = (int) (height * ratio);
                int x = 0;
                if (mWorkspace != null&&mWorkspace.getCurrentPage()>0) {
                    int pageIndex = 0;
                    if(isLiveWallpaper){
                        pageIndex = 1;
                    }else {
                        pageIndex= mWorkspace.getCurrentPage();
                    }
                    int extWidth = (int) (width * (bitmapRatio - ratio));
                    int pageWidth = extWidth / mWorkspace.getPageCount();
                    x = pageIndex * pageWidth;
                }
                try {
                    if(bitmap!=null&&!bitmap.isRecycled()){
                        return Bitmap.createBitmap(bitmap, x, 0, clipBitmapWidth, height);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "create  bitmap error");
                }
            } else if(bitmap!=null&&!bitmap.isRecycled()){
                try {
                    return bitmap/*Bitmap.createBitmap(bitmap)*/;
                }catch (Exception e){
                    Log.e(TAG, "create  bitmap error");
                }
            }
        }
        return null;
    }
    public void openOrCloseFolderAnimation(boolean open,ImageView view){
        if(view == null){
            view = mWorkspaceBgOpenFolder ;
        }
        if(mAniWorkspaceBg!=null&&mAniWorkspaceBg.isRunning()) {
            mAniWorkspaceBg.end();
        }
        if(open){
            view.setImageBitmap(clipBlur(mBlur,false));
            Log.d(TAG,"show blur "+Log.getStackTraceString(new Throwable()));
            mAniWorkspaceBg = animateShowWorkspaceBg(view);
        }else {
            Log.d(TAG,"close blur"+Log.getStackTraceString(new Throwable()));
            mAniWorkspaceBg=dismissWorkspaceBg(view);
        }
        mAniWorkspaceBg.start();
    }
    private Animator animateShowWorkspaceBg(final View view) {
        AnimatorSet animatorSet =LauncherAnimUtils.createAnimatorSet();
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1.0f);
        long duration = getResources().getInteger(R.integer.workspace_bg_ani_duration);


        Interpolator ln = new DecelerateInterpolator(1.5f);
        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(
                view, alpha);
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if(view !=null) {
                    view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                }
                showWorkspaceBackground(view);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if(view !=null) {
                    view.setLayerType(View.LAYER_TYPE_NONE, null);
                }
                mAniWorkspaceBg = null;
            }
        });
        oa.setDuration(duration);
        oa.setInterpolator(ln);
        animatorSet.play(oa);
        return animatorSet;
    }

    private void showWorkspaceBackground(View view) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }
    private Animator dismissWorkspaceBg(final View view) {
        long duration = getResources().getInteger(R.integer.workspace_bg_ani_duration);
        AnimatorSet animatorSet=LauncherAnimUtils.createAnimatorSet();
        Interpolator ln = new AccelerateInterpolator(0.75f);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1.0f, 0f);
        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(
                view, alpha);
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setLayerType(View.LAYER_TYPE_NONE, null);
                if(view !=null){
                    view.setVisibility(View.INVISIBLE);
                }
                mAniWorkspaceBg = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                if(view !=null){
                    view.setVisibility(View.INVISIBLE);
                }
            }
        });
        oa.setDuration(duration);
        oa.setInterpolator(ln);
        animatorSet.play(oa);
        return animatorSet;
    }


    private void checkPermission(){
        List<String> noOkPermissions = new ArrayList<>();
        String [] allPermissions = null;
        if(FeatureFlags.UNREAD_MESSAGE){
            allPermissions = Utilities.concat(sAllPermissions,sUreadPermissions);
        }else {
            allPermissions = sAllPermissions;
        }

        for (String permission : allPermissions) {
            if (ActivityCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_DENIED) {
                noOkPermissions.add(permission);
            }
        }
        if (noOkPermissions.size() <= 0)
            return ;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(noOkPermissions.toArray(new String[noOkPermissions.size()]), REQUEST_PERMISSION_ALL);
        }
    }

    //liuzuo:add the background of workspace when opening folder:end


    //liuzuo:add for folder addIcon:begin
    public void enterImportMode(FolderIcon icon, FolderInfo info){
        mEditFolderIcon = icon;
        mEditFolderInfo = info;
        showImportMode(true);
    }

    private void showImportMode(boolean animated) {
        getStateManager().goToState(IMPORT_MODE);
    }

    public void animationCloseFolder(Folder folder) {
        //mHotseat.setVisibility(View.INVISIBLE);
        //  mFolderImportHintContainer.setVisibility(View.VISIBLE);
        //mFolderImportContainer.setVisibility(View.VISIBLE);
        if(animImportButton!=null)
            animImportButton.cancel();
        animImportButton= showImportButton();
        animImportButton.start();
        // mFolderImportContainer.setTranslationY(mFolderImportContainer.getHeight());
        if(mEditFolderIcon!=null)
            mEditFolderIcon.setAlpha(0.4f);
    }
    private void showFolderIcon(){
        if(mEditFolderIcon != null && mEditFolderIcon.getAlpha() < 1.0f) {
            mEditFolderIcon.setAlpha(1.0f);
        }
    }

    private void exitEditModeAndOpenFolder() {
        clearFolderShortcut();
        boolean snapTo = false;
        if(mOpenFolder && mEditFolderIcon != null ){
            int destScreen = (int) mEditFolderIcon.getFolderInfo().screenId;
            int srcScreen = mWorkspace.getCurrentPage();
            int screenIndex=mWorkspace.getPageIndexForScreenId(destScreen);
            if(destScreen != srcScreen && mEditFolderIcon.getParent() != null
                    && !isHotseatLayout((View)mEditFolderIcon.getParent().getParent())){
                int duration =getResources().getInteger(R.integer.folder_snap_to_page_duration) ;
                snapTo = true;
                mWorkspace.setDelayedSnapToPageRunnable(()->{
                    successExitImportMode(mEditFolderIcon);
                    isSuccessAddIcon = true;
                    exitImportMode(true);

                });
                mWorkspace.snapToPage(screenIndex, duration);
            } else {
                successExitImportMode(mEditFolderIcon);
            }
            mOpenFolder=false;
        } else {
            hideImportMode();
            mEditFolderIcon = null;
        }
        if(!snapTo){
            isSuccessAddIcon = true;
            exitImportMode(true);
        }

    }

    private void successExitImportMode(FolderIcon editFolderIcon){
        hideImportMode();
        handleFolderClick(editFolderIcon);
        mEditFolderIcon = null;
        mWorkspace.stripEmptyScreens();
    }

    private void exitEditModeAndCloseFolder() {
        clearFolderShortcut();
        mWorkspace.stripEmptyScreens();
        if(mOpenFolder && mEditFolderIcon != null ){
            int destScreen = (int) mEditFolderIcon.getFolderInfo().screenId;
            int srcScreen = mWorkspace.getCurrentPage();
            int screenIndex=mWorkspace.getPageIndexForScreenId(destScreen);
            Log.d(TAG,"screenIndex="+screenIndex+" destScreen="+destScreen);
            if(destScreen != srcScreen && mEditFolderIcon.getParent() != null
                    && !isHotseatLayout((View)mEditFolderIcon.getParent().getParent())){
                int duration =getResources().getInteger(R.integer.folder_snap_to_page_duration) ;
                mWorkspace.setDelayedSnapToPageRunnable(()->{
                    hideImportMode();
                    closeFolder(false);
                    if(mEditFolderIcon.getFolder()!=null)
                        mEditFolderIcon.getFolder().close(false);
                    mEditFolderIcon = null;
                });
                mWorkspace.snapToPage(screenIndex, duration);
            } else {
                hideImportMode();
                closeFolder(false);
                if(mEditFolderIcon!=null&&mEditFolderIcon.getFolder()!=null)
                    mEditFolderIcon.getFolder().close(false);
                mEditFolderIcon = null;
            }
            mOpenFolder=false;
        } else {
            hideImportMode();
            mEditFolderIcon = null;
        }
        exitImportMode(true);

    }
    public boolean isSuccessAddIcon() {
        return isSuccessAddIcon;
    }
    private void clearFolderShortcut() {
        if(mCheckedBubbleTextViews != null) {
            for(int i=0;i<mCheckedBubbleTextViews.size();i++){
                mCheckedBubbleTextViews.get(i).setChecked(false);
            }
            mCheckedBubbleTextViews.clear();
        }
        if (mCheckedShortcutInfos!=null)
            mCheckedShortcutInfos.clear();
        Iterator<FolderInfo> iterator = mCheckedFolderInfos.iterator();
        while(iterator.hasNext()){
            FolderInfo info = iterator.next();
            info.clearInfo();
        }
        Iterator<FolderIcon> iteratorIcon = mCheckedFolderIcons.iterator();
        while(iteratorIcon.hasNext()){
            FolderIcon icon = iteratorIcon.next();
            icon.onClearIconInfo();
        }
    }

    private void handleFolderClick(FolderIcon editFolderIcon) {
        if(editFolderIcon==null)
            return;
        final FolderInfo info = editFolderIcon.getFolderInfo();
        Folder openFolder = mWorkspace.getFolderForTag(info);
        editFolderIcon.getFolder().mSnapToLastpage=true;
        // If the folder info reports that the associated folder is open, then
        // verify that
        // it is actually opened. There have been a few instances where this
        // gets out of sync.
/*        if (info.opened && openFolder == null) {
            info.opened = false;
        }*/

        if (!editFolderIcon.getFolder().isDestroyed()) {
            // Close any open folder
            closeFolder(false);
            // Open the requested folder
            openFolder(editFolderIcon);
        } else {
            // Find the open folder...
            int folderScreen;
            if (openFolder != null) {
                folderScreen = mWorkspace.getPageForView(openFolder);
                // .. and close it
                closeFolder(openFolder,false);
                if (folderScreen != mWorkspace.getCurrentPage()) {
                    // Close any folder open on the current screen
                    closeFolder(true);
                    // Pull the folder onto this screen
                    openFolder(editFolderIcon);

                }
            }
        }
    }

    private void hideImportMode() {
        if(animImportButton!=null)
            animImportButton.cancel();
        animImportButton= hideImportButton();
        animImportButton.start();
        //mFolderImportContainer.setVisibility(View.INVISIBLE);
    }
    private void setImportMode(boolean b) {
        if(mEditFolderIcon==null) {
            Log.e(TAG, "Launcher setImportMode is error"+ android.util.Log.getStackTraceString(new Throwable()));
            return;
        }
        mEditFolderIcon.getFolder().setImportMode(b);
    }
    public boolean getImportMode(){
        return mEditFolderIcon!=null&&mEditFolderIcon.getFolder().isImportMode();
    }
    public void addCheckedFolderInfo(FolderInfo info){
        if (!mCheckedFolderInfos.contains(info)) {
            mCheckedFolderInfos.add(info);
        }
    }
    public void addCheckedFolderIcon(FolderIcon icon){
        if (!mCheckedFolderIcons.contains(icon)) {
            mCheckedFolderIcons.add(icon);
        }
    }

    public void updateImportButton() {
        mFolderImportButton.setText(getString(R.string.folder_importmode_button,mCheckedShortcutInfos.size()));
        if(mCheckedShortcutInfos.size() > 0){
            mFolderImportButton.getBackground().setAlpha(226);
            mFolderImportButton.setEnabled(true);
        } else {
            mFolderImportButton.getBackground().setAlpha(127);
            mFolderImportButton.setEnabled(false);
        }
    }
    private void exitImportMode(boolean animated) {
        getStateManager().goToState(NORMAL);
        isSuccessAddIcon = false;
    }

    private Animator showImportButton() {
        AnimatorSet ain= LauncherAnimUtils.createAnimatorSet();
        ain.play(createImportButtonAnimation(mFolderImportContainer));
        return ain;
    }
    private Animator hideImportButton() {
        long duration = getResources().getInteger(R.integer.folder_blur_background_duration);
        Interpolator ln = new AccelerateDecelerateInterpolator();
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1.0f, 0f);
        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(
                mFolderImportContainer, alpha);
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mFolderImportContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mFolderImportContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                if(mFolderImportContainer!=null){
                    //mFolderBlurBackground.setBackground(null);
                    mFolderImportContainer.setVisibility(View.INVISIBLE);
                }
            }
        });
        oa.setInterpolator(ln);
        oa.setDuration(duration);
        oa.setStartDelay(getResources().getInteger(R.integer.folder_import_button_startdelay));
        return oa;
    }
    private Animator createImportButtonAnimation(final View view){
        long duration = getResources().getInteger(R.integer.folder_blur_background_duration);
        Interpolator ln = new AccelerateDecelerateInterpolator();
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(
                view, alpha);
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setVisibility(View.VISIBLE);
                view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
        oa.setDuration(duration);
        oa.setInterpolator(ln);
        oa.setStartDelay(getResources().getInteger(R.integer.folder_import_button_startdelay));
        return oa;
    }
    public void closeFolder(boolean animate) {
        Log.d("liuzuo99","closeFolder"+PlatformFeatureFlags.SUPPORT_FOLDER_TAP_MODE);
        if(PlatformFeatureFlags.SUPPORT_FOLDER_TAP_MODE){
            if (mFolderPagesContainer.isOpen()) {
                mFolderPagesContainer.animateClose(this::checkPermission);
            } else {
          /*  if (r != null) {
                r.run();
            }*/
            }
        }else {
            Folder folder = Folder.getOpen(this);
            if (folder != null) {
                if (folder.isEditingName()) {
                    folder.dismissEditingName();
                }
                closeFolder(folder, animate);
            }
        }


    }

    public void closeFolder(Folder folder, boolean animate) {
        folder.close(animate);
    }
    public void openFolder(FolderIcon folderIcon) {
        hideImportMode();
        Folder folder = folderIcon.getFolder();
        Folder openFolder = Folder.getOpen(this);
        if (openFolder != null && openFolder != folder) {
            // Close any open folder before opening a folder.
            closeFolder(true);
        }

        FolderInfo info = folder.mInfo;


        // While the folder is open, the position of the icon cannot change.
        ((CellLayout.LayoutParams) folderIcon.getLayoutParams()).canReorder = false;

        // Just verify that the folder hasn't already been added to the DragLayer.
        // There was a one-off crash where the folder had a parent already.
        if (folder.getParent() == null) {
            mDragLayer.addView(folder);
            mDragController.addDropTarget(folder);
        } else {
            Log.w(TAG, "Opening folder (" + folder + ") which already has a parent (" +
                    folder.getParent() + ").");
        }

        folder.animateOpen();


        // Notify the accessibility manager that this folder "window" has appeared and occluded
        // the workspace items
        folder.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        getDragLayer().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);

    }
    private void forceExitImportMode(){
        showFolderIcon();
        mOpenFolder =true;
        if(mCheckedBubbleTextViews!=null){
            for (BubbleTextView bv:mCheckedBubbleTextViews
                    ) {
                bv.setChecked(false);
            }
        }
        mEditFolderIcon.getFolder().setImportMode(false);
        mCheckedBubbleTextViews.clear();
        mCheckedShortcutInfos.clear();
        exitImportModeInHomeKey=true;
        exitEditModeAndCloseFolder();
        exitImportModeInHomeKey=false;
    }

    public void onImportModeClick(View v,ShortcutInfo tag){
        BubbleTextView bv=  (BubbleTextView)v;
        if(bv.isChecked()){
            bv.setChecked(false);
            mCheckedShortcutInfos.remove((ShortcutInfo)tag);
            mCheckedBubbleTextViews.remove(bv);
            updateImportButton();
        }else {
            bv.setChecked(true);
            Log.d(TAG,"info="+tag.toString());
            mCheckedShortcutInfos.add((ShortcutInfo)tag);
            mCheckedBubbleTextViews.add(bv);
            updateImportButton();
        }
    }
    public void onImportBtnClick(){
        closeFolder(true);
        setImportMode(false);
        int i=0;
        for(ShortcutInfo info :mCheckedShortcutInfos){
            View view=mWorkspace.getParentCellLayoutForView(mCheckedBubbleTextViews.get(i));
            Log.d(TAG,"info.screenId="+info.toString());
            if(view instanceof CellLayout){
                mCheckedBubbleTextViews.get(i).setChecked(false);
                ((CellLayout) view).removeView(mCheckedBubbleTextViews.get(i));
            }
            i++;
        }
        Log.d(TAG,"mCheckedFolderInfos="+mCheckedFolderInfos.toString());
        Iterator<FolderInfo> iterator = mCheckedFolderInfos.iterator();
        while(iterator.hasNext()){
            FolderInfo info = iterator.next();
            info.removeInfo();
        }
        Iterator<FolderIcon> iteratorIcon = mCheckedFolderIcons.iterator();
        while(iteratorIcon.hasNext()){
            FolderIcon icon = iteratorIcon.next();
            icon.removeInfo();
        }
        mEditFolderInfo.addInfo(mCheckedShortcutInfos);
        mOpenFolder = true;
        showFolderIcon();
        exitEditModeAndOpenFolder();
    }

    public FolderIcon getEditFolderIcon() {
        return mEditFolderIcon;
    }

    public Button getFolderImportButton() {
        return mFolderImportButton;
    }

    //liuzuo:add for folder addIcon:end
//liuzuo:add for visible of hotseat:begin
    private boolean getHotseatVisible(){
        return getHotseat().getVisibility()==View.INVISIBLE||getHotseat().getAlpha()==0;
    }
    public void workspaceAnimationForCloseFolder(Folder folder){
        if(getImportMode()){
            animationCloseFolder(folder);
        }else if(getHotseatVisible()){
            //showWorkspace(true);
        }
    }

    //liuzuo:add for visible of hotseat:end

    //liuzuo:change widget UI:begin

    public Bitmap getBlur() {
        return mBlur;
    }
    //liuzuo:change widget UI:end

    // Perry: Add edit mode panel: start
    public EditModePanel getEditModePanel() {
        return mEditModePanel;
    }
    // Perry: Add edit mode panel: end

    // Perry: Add predicted applications: start
    public void tryAndUpdatePredictedApps() {}
    public List<ComponentKey> getPredictedApps() {
        return Collections.emptyList();
    }
    // Perry: Add predicted applications: end

    // Perry: Add gesture for some states: start
    public void onRecentsDisplay() {
        mLauncherCallbacks.onRecentsDisplay();
    }
    // Perry: Add gesture for some states: end

    // liuzuo: Add  for ace folder: start
    public boolean isInEditMode() {
        return isInState(EDIT_MODE);
    }
    public FolderPagesContainer getFolderPagesContainer() {
        return mFolderPagesContainer;
    }
    public void setWorkspaceAlpha(float alpha, boolean forAllPages) {
        mWorkspace.setWorkspaceAlpha(alpha, forAllPages);
    }
    // liuzuo: Add  for ace folder: end

    //Bruce : unread message : start
    private void setMmsOrPhoneNum(final ComponentName cName, final int missCount, final int type) {
//        Log.i("zwz", "componentName = " + cName.toString() + " missCount = " + missCount);
        if (mWorkspace == null) return;
        ArrayList<CellLayout> cellLayouts = mWorkspace.getWorkspaceAndHotseatCellLayouts();
        for (final CellLayout layoutParent : cellLayouts) {
            final ViewGroup shortcutAndWidgetContainer = layoutParent.getShortcutsAndWidgets();
            mWorkspace.post(new Runnable() {
                public void run() {
                    int childCount = shortcutAndWidgetContainer.getChildCount();
                    for (int j = 0; j < childCount; j++) {
                        View view = shortcutAndWidgetContainer.getChildAt(j);
                        Object tag = view.getTag();
                        if (tag instanceof ShortcutInfo) {
                            final ShortcutInfo info = (ShortcutInfo) tag;
                            final Intent intent = info.intent;
                            if (intent != null) {
                                final ComponentName name = intent.getComponent();
                                if (name != null && name.equals(cName)) {
                                    BubbleTextView bv = (BubbleTextView) view;
                                    bv.drawUnreadIcon(missCount, type);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private void checkUnreadPermission() {
        List<String> noOkPermissions = new ArrayList<>();

        for (String permission : sUreadPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                noOkPermissions.add(permission);
            }
        }
        if (noOkPermissions.size() <= 0)
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(noOkPermissions.toArray(new String[noOkPermissions.size()]), REQUEST_PERMISSION_UNREAD);
        }
    }
    private void initUnreadMessage() {
        //checkUnreadPermission();
        UnreadInfoManager.getInstance(this).setUnreadInfoInterface(new UnreadInfoManager.UnreadInfoInterface() {
//            @RequiresApi(api = Build.VERSION_CODES.M)
//            @Override
//            public void requestUnreadPermise(String[] deniedString, int requestCode) {
//                requestPermissions(deniedString, requestCode);
//            }

            @Override
            public void updateUnreadBadges(ComponentName componentName, int count, int type) {
                //Log.d("liuzuo98","updateUnreadBadges  componentName="+componentName+" ,count="+count+",type="+type);
                setMmsOrPhoneNum(componentName, count, type);
            }
        });
        UnreadInfoManager.getInstance(this).init();
    }
    //Bruce : unread message : end

    // Perry: Implement frozen apps: start
    public boolean isInFrozenState() {
        return isInState(ALL_APPS) && mAppsView.isInState(FROZEN);
    }
    // Perry: Implement frozen apps: end
}
