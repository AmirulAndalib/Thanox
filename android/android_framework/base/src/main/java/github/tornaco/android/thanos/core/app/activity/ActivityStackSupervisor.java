package github.tornaco.android.thanos.core.app.activity;

import android.content.ComponentName;
import android.os.RemoteException;

import java.util.List;

import github.tornaco.android.thanos.core.app.component.ComponentReplacement;
import github.tornaco.android.thanos.core.pm.Pkg;
import lombok.SneakyThrows;

public class ActivityStackSupervisor {
    public static final class LaunchOtherAppPkgSetting {
        public static final int ALLOW_LISTED = 2;
        public static final int ALLOW = 0;
        public static final int ASK = 1;
        public static final int IGNORE = -1;
    }

    public static final class LockMethod {
        public static final int SYSTEM = 0;
        public static final int PATTERN = 1;
        public static final int PIN = 2;
    }

    private final IActivityStackSupervisor supervisor;

    public ActivityStackSupervisor(IActivityStackSupervisor supervisor) {
        this.supervisor = supervisor;
    }

    @SneakyThrows
    public boolean shouldVerifyActivityStarting(ComponentName componentName, String pkg, String source) {
        return supervisor.shouldVerifyActivityStarting(componentName, pkg, source);
    }

    @SneakyThrows
    public String getCurrentFrontApp() {
        return supervisor.getCurrentFrontApp();
    }

    @SneakyThrows
    public void setAppLockEnabled(boolean enabled) {
        supervisor.setAppLockEnabled(enabled);
    }

    @SneakyThrows
    public boolean isAppLockEnabled() {
        return supervisor.isAppLockEnabled();
    }

    @SneakyThrows
    public boolean isPackageLocked(String pkg) {
        return supervisor.isPackageLocked(pkg);
    }

    @SneakyThrows
    public void setPackageLocked(String pkg, boolean locked) {
        supervisor.setPackageLocked(pkg, locked);
    }

    @SneakyThrows
    public void setVerifyResult(int request, int result, int reason) {
        supervisor.setVerifyResult(request, result, reason);
    }

    @SneakyThrows
    public void addComponentReplacement(ComponentReplacement replacement) {
        supervisor.addComponentReplacement(replacement);
    }

    @SneakyThrows
    public void removeComponentReplacement(ComponentReplacement replacement) {
        supervisor.removeComponentReplacement(replacement);
    }

    @SneakyThrows
    public List<ComponentReplacement> getComponentReplacements() {
        return supervisor.getComponentReplacements();
    }

    @SneakyThrows
    public void setActivityTrampolineEnabled(boolean enabled) {
        supervisor.setActivityTrampolineEnabled(enabled);
    }

    @SneakyThrows
    public boolean isActivityTrampolineEnabled() {
        return supervisor.isActivityTrampolineEnabled();
    }

    @SneakyThrows
    public void setShowCurrentComponentViewEnabled(boolean enabled) {
        supervisor.setShowCurrentComponentViewEnabled(enabled);
    }

    @SneakyThrows
    public boolean isShowCurrentComponentViewEnabled() {
        return supervisor.isShowCurrentComponentViewEnabled();
    }

    @SneakyThrows
    public void registerTopPackageChangeListener(TopPackageChangeListener listener) {
        supervisor.registerTopPackageChangeListener(listener.getListener());
    }

    @SneakyThrows
    public void unRegisterTopPackageChangeListener(TopPackageChangeListener listener) {
        supervisor.unRegisterTopPackageChangeListener(listener.getListener());
    }

    @SneakyThrows
    public boolean isVerifyOnScreenOffEnabled() {
        return supervisor.isVerifyOnScreenOffEnabled();
    }

    @SneakyThrows
    public void setVerifyOnScreenOffEnabled(boolean enabled) {
        supervisor.setVerifyOnScreenOffEnabled(enabled);
    }

    @SneakyThrows
    public boolean isVerifyOnAppSwitchEnabled() {
        return supervisor.isVerifyOnAppSwitchEnabled();
    }

    @SneakyThrows
    public void setVerifyOnAppSwitchEnabled(boolean enabled) {
        supervisor.setVerifyOnAppSwitchEnabled(enabled);
    }

    @SneakyThrows
    public boolean isVerifyOnTaskRemovedEnabled() {
        return supervisor.isVerifyOnTaskRemovedEnabled();
    }

    @SneakyThrows
    public void setVerifyOnTaskRemovedEnabled(boolean enabled) {
        supervisor.setVerifyOnTaskRemovedEnabled(enabled);
    }

    @SneakyThrows
    public void registerActivityLifecycleListener(IActivityLifecycleListener listener) {
        supervisor.registerActivityLifecycleListener(listener);
    }

    @SneakyThrows
    public void unRegisterActivityLifecycleListener(IActivityLifecycleListener listener) {
        supervisor.unRegisterActivityLifecycleListener(listener);
    }

    @SneakyThrows
    public void addAppLockWhiteListComponents(List<ComponentName> componentName) {
        supervisor.addAppLockWhiteListComponents(componentName);
    }

    @SneakyThrows
    public void removeAppLockWhiteListComponents(List<ComponentName> componentName) {
        supervisor.removeAppLockWhiteListComponents(componentName);
    }

    @SneakyThrows
    public List<ComponentName> getAppLockWhiteListComponents() {
        return supervisor.getAppLockWhiteListComponents();
    }

    @SneakyThrows
    public int getLaunchOtherAppSetting(Pkg pkg) {
        return supervisor.getLaunchOtherAppSetting(pkg);
    }

    @SneakyThrows
    public void setLaunchOtherAppSetting(Pkg pkg, int setting) {
        supervisor.setLaunchOtherAppSetting(pkg, setting);
    }

    @SneakyThrows
    public boolean isLaunchOtherAppBlockerEnabled() {
        return supervisor.isLaunchOtherAppBlockerEnabled();
    }

    @SneakyThrows
    public void setLaunchOtherAppBlockerEnabled(boolean enable) {
        supervisor.setLaunchOtherAppBlockerEnabled(enable);
    }

    @SneakyThrows
    public void addLaunchOtherAppRule(String rule) throws RemoteException {
        supervisor.addLaunchOtherAppRule(rule);
    }

    @SneakyThrows
    public void deleteLaunchOtherAppRule(String rule) throws RemoteException {
        supervisor.deleteLaunchOtherAppRule(rule);
    }

    @SneakyThrows
    public String[] getAllLaunchOtherAppRules() throws RemoteException {
        return supervisor.getAllLaunchOtherAppRules();
    }

    @SneakyThrows
    public void removePkgFromLaunchOtherAppAllowList(Pkg pkg, Pkg pkgToRemove) {
        supervisor.removePkgFromLaunchOtherAppAllowList(pkg, pkgToRemove);
    }

    @SneakyThrows
    public void addPkgToLaunchOtherAppAllowList(Pkg pkg, Pkg pkgToAdd) {
        supervisor.addPkgToLaunchOtherAppAllowList(pkg, pkgToAdd);
    }

    @SneakyThrows
    public List<Pkg> getLaunchOtherAppAllowListOrNull(Pkg callerPkg) {
        return supervisor.getLaunchOtherAppAllowListOrNull(callerPkg);
    }

    @SneakyThrows
    public void setLockMethod(int method) {
        supervisor.setLockMethod(method);
    }

    @SneakyThrows
    public void setLockPattern(String pattern) {
        supervisor.setLockPattern(pattern);
    }

    @SneakyThrows
    public String getLockPattern() {
        return supervisor.getLockPattern();
    }

    @SneakyThrows
    public void setLockPin(String pin) {
        supervisor.setLockPin(pin);
    }

    @SneakyThrows
    public String getLockPin() {
        return supervisor.getLockPin();
    }

    @SneakyThrows
    public int getLockMethod() {
        return supervisor.getLockMethod();
    }

    @SneakyThrows
    public void setLockPatternLineHidden(boolean hidden) {
        supervisor.setLockPatternLineHidden(hidden);
    }

    @SneakyThrows
    public boolean isLockPatternLineHidden() {
        return supervisor.isLockPatternLineHidden();
    }
}
