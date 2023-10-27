package com.ombut.ironsourceplugin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.godotengine.godot.Godot;
import org.godotengine.godot.GodotLib;
import org.godotengine.godot.plugin.UsedByGodot;
import org.godotengine.godot.plugin.SignalInfo;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.util.Log;
import java.util.*;

import android.widget.Toast;
import android.widget.FrameLayout;
import android.view.ViewGroup;

import androidx.collection.ArraySet;


import com.ironsource.adapters.supersonicads.SupersonicConfig;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.integration.IntegrationHelper;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.BannerListener;
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener;
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener;
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener;
import com.ironsource.mediationsdk.sdk.RewardedVideoListener;
import com.ironsource.mediationsdk.sdk.InterstitialListener;
import com.ironsource.mediationsdk.utils.IronSourceUtils;

public class IronSourcePlugin extends org.godotengine.godot.plugin.GodotPlugin {
    private String TAG = "godot";
    private String APP_KEY = "";

    private Activity activity = null; // The main activity of the game

    private Placement mPlacement;

    private FrameLayout mBannerParentLayout = null;
    private IronSourceBannerLayout mIronSourceBannerLayout = null;
    private FrameLayout.LayoutParams mAdParams = null;

    public IronSourcePlugin(Godot godot) {
        super(godot);
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "IronSourcePlugin";
    }

    @Nullable
    @Override
    public View onMainCreate(Activity av) {
        activity = av;
        mBannerParentLayout = new FrameLayout(activity);
        return mBannerParentLayout;
    }

    public void onMainResume() {
        IronSource.onResume(activity);
        super.onMainResume();
    }

    public void onMainPause() {
        IronSource.onPause(activity);
        super.onMainPause();
    }

    //----------------------------------------------------------------------------------------------
    @UsedByGodot
    public void init(String appKey,
                     final String[] chosen_ad_units,
                     final boolean validateIntegration,
                     final boolean lanchTestSuit
    ) {


        IronSource.shouldTrackNetworkState(activity, true);

        if (validateIntegration){
            IntegrationHelper.validateIntegration(activity); // TODO: REMOVE BEFORE GOING LIVE
        }

        if (lanchTestSuit){
            //IronSource.setMetaData("is_test_suite", "enable");
        }

        APP_KEY = appKey;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                IronSource.init(
                        activity,
                        APP_KEY,
                        IronSource.AD_UNIT.REWARDED_VIDEO,
                        IronSource.AD_UNIT.BANNER,
                        IronSource.AD_UNIT.INTERSTITIAL
                );
                if (Arrays.asList(chosen_ad_units).contains("REWARDED_VIDEO")) {
                    setRewardedVideoListeners();
                    IronSource.init(activity, APP_KEY, IronSource.AD_UNIT.REWARDED_VIDEO);
                }
                if (Arrays.asList(chosen_ad_units).contains("BANNER")) {
                    IronSource.init(activity, APP_KEY, IronSource.AD_UNIT.BANNER);
                    createAndloadBanner();
                }
                if (Arrays.asList(chosen_ad_units).contains("INTERSTITIAL")) {
                    setInterstitialListeners();
                    IronSource.init(activity, APP_KEY, IronSource.AD_UNIT.INTERSTITIAL);
                }
            }
        });
    }

    // --------- REWARDED VIDEO ---------


    private void setRewardedVideoListeners() {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                IronSource.setLevelPlayRewardedVideoListener(new LevelPlayRewardedVideoListener() {
                    @Override
                    public void onAdOpened(AdInfo adInfo) {

                    }

                    @Override
                    public void onAdShowFailed(IronSourceError error, AdInfo adInfo) {
                        emitSignal(
                                "on_rewarded_video_ad_show_failed",
                                error.toString()
                        );
                    }

                    @Override
                    public void onAdClicked(Placement placement, AdInfo adInfo) {

                    }

                    @Override
                    public void onAdRewarded(Placement placement, AdInfo adInfo) {
                        mPlacement = placement;
                    }

                    @Override
                    public void onAdClosed(AdInfo adInfo) {
                        if (mPlacement != null) {
                            // if the user was rewarded
                            emitSignal(
                                    "on_rewarded_video_ad_closed",
                                    mPlacement.getRewardName(),
                                    mPlacement.getRewardAmount()
                            );
                            mPlacement = null;
                        }else{
                            emitSignal(
                                    "on_rewarded_video_ad_closed",
                                    null,
                                    0
                            );
                        }
                    }

                    @Override
                    public void onAdAvailable(AdInfo adInfo) {
                        emitSignal(
                                "on_rewarded_availability_changed",
                                true
                        );
                    }

                    @Override
                    public void onAdUnavailable() {
                        emitSignal(
                                "on_rewarded_availability_changed",
                                false
                        );
                    }
                });
            }
        });
    }
    @UsedByGodot
    public void loadRewardedVideo(){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                IronSource.loadRewardedVideo();
            }
        });
    }
    @UsedByGodot
    public void showRewardedVideo(final String placementName) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (IronSource.isRewardedVideoAvailable()) {
                    IronSource.showRewardedVideo(placementName);
                }
            }
        });
    }
    // --------- INTERSTITIAL ---------

    private void setInterstitialListeners() {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                IronSource.setLevelPlayInterstitialListener(new LevelPlayInterstitialListener() {
                    @Override
                    public void onAdReady(AdInfo adInfo) {
                        emitSignal(
                                "on_interstitial_ad_ready"
                        );
                    }

                    @Override
                    public void onAdLoadFailed(IronSourceError ironSourceError) {
                        emitSignal(
                            "on_interstitial_ad_load_failed",
                            ironSourceError.toString()
                        );
                    }

                    @Override
                    public void onAdOpened(AdInfo adInfo) {

                    }

                    @Override
                    public void onAdShowSucceeded(AdInfo adInfo) {

                    }

                    @Override
                    public void onAdShowFailed(IronSourceError ironSourceError, AdInfo adInfo) {
                        emitSignal(
                                "on_interstitial_ad_show_failed",
                                ironSourceError.toString()
                        );
                    }

                    @Override
                    public void onAdClicked(AdInfo adInfo) {

                    }

                    @Override
                    public void onAdClosed(AdInfo adInfo) {

                    }
                });
            }
        });
    }
    @UsedByGodot
    public void loadInterstitial() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                IronSource.loadInterstitial();
            }
        });
    }
    @UsedByGodot
    private void showInterstitial() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (IronSource.isInterstitialReady()) {
                    IronSource.showInterstitial();
                }
            }
        });
    }

    // --------- BANNER ---------

    private void createAndloadBanner() {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                mIronSourceBannerLayout = IronSource.createBanner(activity, ISBannerSize.SMART);
                mAdParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT);

                mAdParams.gravity = Gravity.BOTTOM;
                activity.addContentView(mIronSourceBannerLayout, mAdParams);

                if (mIronSourceBannerLayout != null) {

                    ViewGroup parent = (ViewGroup) mIronSourceBannerLayout.getParent();
                    if (parent != null) {
                        parent.removeView(mIronSourceBannerLayout);
                    }
                    mBannerParentLayout.removeView(mIronSourceBannerLayout); // Remove the old view
                }

                mBannerParentLayout.addView(mIronSourceBannerLayout, 0, mAdParams);

                if (mIronSourceBannerLayout != null) {
                    mIronSourceBannerLayout.setLevelPlayBannerListener(new LevelPlayBannerListener() {
                        @Override
                        public void onAdLoaded(AdInfo adInfo) {
                            mBannerParentLayout.setVisibility(View.VISIBLE);
                            emitSignal("on_banner_ad_loaded");
                        }

                        @Override
                        public void onAdLoadFailed(IronSourceError error) {
                            emitSignal("on_banner_ad_load_failed", error.toString());
                        }

                        @Override
                        public void onAdClicked(AdInfo adInfo) {

                        }

                        @Override
                        public void onAdLeftApplication(AdInfo adInfo) {

                        }

                        @Override
                        public void onAdScreenPresented(AdInfo adInfo) {

                        }

                        @Override
                        public void onAdScreenDismissed(AdInfo adInfo) {

                        }

                    });
                    IronSource.loadBanner(mIronSourceBannerLayout);
                }
            }
        });
    }

    private void destroyAndDetachBanner() {
        IronSource.destroyBanner(mIronSourceBannerLayout);
        if (mBannerParentLayout != null) {
            mBannerParentLayout.removeView(mIronSourceBannerLayout);
        }
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new ArraySet<>();

        signals.add(new SignalInfo("on_banner_ad_loaded"));
        signals.add(new SignalInfo("on_banner_ad_load_failed", String.class));

        signals.add(new SignalInfo("on_rewarded_video_ad_show_failed", String.class));
        signals.add(new SignalInfo("on_rewarded_video_ad_closed", String.class, Integer.class));
        signals.add(new SignalInfo("on_rewarded_availability_changed", Boolean.class));

        signals.add(new SignalInfo("on_interstitial_ad_ready"));
        signals.add(new SignalInfo("on_interstitial_ad_load_failed", String.class));
        signals.add(new SignalInfo("on_interstitial_ad_show_failed", String.class));

        return signals;
    }
}
