/*
 * Created by Aditya Nathwani on 5/12/20 4:45 PM
 *  Copyright (c) 2020 .
 * Last modified 5/12/20 4:33 PM
 */

package in.omdev.nsdhelper;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.util.ArrayList;

/**
 * This class abstracts operations performed by {@link NsdManager} like registering
 * and discovering local network services.
 */
public class NsdHelper {

    public static final String TAG = "NsdHelper";
    /**
     * Error type passed in {@link NsdErrorListener} indicates that
     * service registration failed
     * using {@link NsdHelper#register(NsdServiceInfo serviceInfo)}.
     */
    public static final int ERROR_TYPE_REGISTRATION_FAILED = 1;

    /**
     * Error type passed in {@link NsdErrorListener} indicates that
     * service unregistration failed using {@link NsdHelper#unregister()}.
     */
    public static final int ERROR_TYPE_UNREGISTRATION_FAILED = 2;

    /**
     * Error type passed in {@link NsdErrorListener} indicates that
     * starting service discovery failed
     * using {@link NsdHelper#discover(String serviceType)}.
     */
    public static final int ERROR_TYPE_START_DISCOVERY_FAILED = 3;

    /**
     * Error type passed in {@link NsdErrorListener} indicates that
     * stopping service discovery failed using {@link NsdHelper#stopDiscovery()}.
     */
    public static final int ERROR_TYPE_STOP_DISCOVERY_FAILED = 4;

    /**
     * Error type passed in {@link NsdErrorListener} indicates that
     * resolving {@link NsdServiceInfo} failed using {@link NsdHelper#stopDiscovery()}.
     */
    private static final int ERROR_TYPE_RESOLVE_FAILED = 5;

    private final Activity activity;
    private final NsdManager nsdManager;
    private final ArrayList<NsdServiceInfo> services = new ArrayList<>();
    private NsdErrorListener errorListener;
    private boolean registering = false;
    private boolean registered = false;
    private boolean reregister = false;
    private NsdServiceInfo serviceInfo;
    private String name;
    private NsdRegistrationListener registrationListener;
    private boolean discovering = false;
    private boolean discoveryStarting = false;
    private boolean rediscover = false;
    private String serviceType;
    private boolean excludeOwnService = false;
    private NsdDiscoveryListener discoveryListener;
    private ServiceListener serviceListener;

    private ResolveListener resolveListener;

    /**
     * Initialize {@link NsdHelper}.
     *
     * @param activity used to get {@link NsdManager}. Callbacks of {@link NsdHelper} will
     *                 be called using {@link Activity#runOnUiThread(Runnable callback)}
     */
    public NsdHelper(Activity activity) {
        if (activity == null) {
            throw new NullPointerException("activity null.");
        }
        nsdManager = (NsdManager) activity.getSystemService(Context.NSD_SERVICE);
        this.activity = activity;
    }

    /**
     * Gets error message corresponding to errorCode passed in
     * {@link NsdErrorListener#onError(int errorType, int errorCode)}
     *
     * @param errorCode can be either of {@link NsdManager#FAILURE_ALREADY_ACTIVE},
     *                  {@link NsdManager#FAILURE_INTERNAL_ERROR} and
     *                  {@link NsdManager#FAILURE_MAX_LIMIT}
     * @return Error message
     */
    public static String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case NsdManager.FAILURE_ALREADY_ACTIVE:
                return "The operation failed because it is already active.";
            case NsdManager.FAILURE_INTERNAL_ERROR:
                return "Internal error.";
            case NsdManager.FAILURE_MAX_LIMIT:
                return "The operation failed because the maximum outstanding requests" +
                        " from the applications have reached.";
            default:
                return "Unknown error.";
        }
    }

    /**
     * Sets listener to be invoked when an error occurs in
     * any of the {@link NsdHelper} operations
     *
     * @param errorListener listener to be invoked when any error occurs.
     */
    public void setErrorListener(NsdErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    /**
     * Call this method before calling {@link NsdHelper#discover(String serviceType)}
     * to exclude own service which might have been registered using
     * {@link NsdHelper#register(NsdServiceInfo serviceInfo)}
     *
     * @param excludeOwnService set true to exclude own service.
     *                          Default is false.
     */
    public void setExcludeOwnService(boolean excludeOwnService) {
        this.excludeOwnService = excludeOwnService;
    }

    /**
     * Sets listener to be called when a new service is found
     * or an already found service is lost. Set this before calling
     * {@link NsdHelper#discover(String serviceType)}
     *
     * @param serviceListener listener to be called.
     */
    public void setServiceListener(ServiceListener serviceListener) {
        this.serviceListener = serviceListener;
    }

    /**
     * Sets listener to be called when a service is resolved using
     * {@link NsdHelper#resolve(NsdServiceInfo serviceInfo)}
     *
     * @param resolveListener listener to be called
     */
    public void setResolveListener(ResolveListener resolveListener) {
        this.resolveListener = resolveListener;
    }

    /**
     * Gets name of the own service registered using
     * {@link NsdHelper#register(NsdServiceInfo serviceInfo)}.
     * This name might be different than the name set using
     * {@link NsdServiceInfo#setServiceName(String s)} to avoid
     * conflict. Normally this name is used to filter out own
     * registered service which can be automatically done using
     * {@link NsdHelper#setExcludeOwnService(boolean exclude)}
     *
     * @return name of the registered service
     */
    public String getName() {
        return name;
    }


    /**
     * Get a cloned list of active services found using
     * {@link NsdHelper#discover(String serviceType)}. If any service
     * is lost, it will not be removed from the returned cloned list. Thus,
     * to get the updated list, you will have to call this method
     * again.
     *
     * @return cloned list of services found using
     * {@link NsdHelper#discover(String serviceType)}.
     */
    @SuppressWarnings("unchecked")
    public ArrayList<NsdServiceInfo> getServices() {
        return (ArrayList<NsdServiceInfo>) services.clone();
    }

    /**
     * Register a dns-sd service. If any service is already registered,
     * it will be unregistered before registering new service. This service
     * will be visible to any device connected in local network.
     *
     * @param serviceInfo {@link NsdServiceInfo} object which contains connection
     *                    type and the service name.
     */
    public void register(NsdServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            throw new NullPointerException("serviceInfo null. Set serviceInfo " +
                    "using setServiceInfo()");
        }
        if (!registering) {
            if (registered) {
                reregister = true;
                this.serviceInfo = serviceInfo;
                unregister();
            } else {
                if (registrationListener == null) {
                    registrationListener = new NsdRegistrationListener();
                    registering = true;
                    nsdManager.registerService(
                            serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
                }
            }
        }
    }

    /**
     * Unregister the service registered using
     * {@link NsdHelper#register(NsdServiceInfo serviceInfo)}.
     * Calling this method have no effect is no service is registered.
     */
    public void unregister() {
        if (registered && registrationListener != null) {
            nsdManager.unregisterService(registrationListener);
            registrationListener = null;
        }
    }

    /**
     * Discover services in local network. If discovery is already started, this
     * method will stop discovery, clear the service list and start discovery again.
     * Register callback {@link ServiceListener} before calling this method to get
     * notified of found and lost services. Call {@link NsdHelper#getServices()} to
     * get a list of active services found using this method.
     * @param serviceType service type to discover in the local network.
     *                    e.g "_myService._tcp"
     */
    public void discover(String serviceType) {
        services.clear();
        if (!discoveryStarting) {
            if (discovering) {
                rediscover = true;
                this.serviceType = serviceType;
                stopDiscovery();
            } else {
                if (discoveryListener == null) {
                    discoveryListener = new NsdDiscoveryListener();
                    discoveryStarting = true;
                    nsdManager.discoverServices(serviceType,
                            NsdManager.PROTOCOL_DNS_SD,
                            discoveryListener);
                }
            }
        }
    }

    /**
     * Stop the service discovery started using
     * {@link NsdHelper#discover(String srviceType)}. Calling this method
     * will have no effect if discovery is not already started.
     */
    public void stopDiscovery() {
        if (discovering && discoveryListener != null) {
            nsdManager.stopServiceDiscovery(discoveryListener);
            discoveryListener = null;
        }
    }

    /**
     * Resolve a service found using
     * {@link NsdHelper#discover(String deviceType)}. A found service needs to
     * be resolved to get its details like port number and and attributes.
     *
     * @param serviceInfo passed in
     * {@link ServiceListener#onServiceFound(NsdServiceInfo serviceInfo)}. Can
     * be found in a list returned be {@link NsdHelper#getServices()}.
     */
    public void resolve(NsdServiceInfo serviceInfo) {
        nsdManager.resolveService(serviceInfo, new NsdResolveListener());
    }

    /**
     * Callback interface which is invoked to indicate an error
     * in NsdHelper operations.
     */
    public interface NsdErrorListener {
        /**
         * Called when there is an error in any of NsdHelper operations.
         * @param errorType can be one of
         *                  {@link NsdHelper#ERROR_TYPE_REGISTRATION_FAILED}
         *                  {@link NsdHelper#ERROR_TYPE_UNREGISTRATION_FAILED}
         *                  {@link NsdHelper#ERROR_TYPE_START_DISCOVERY_FAILED}
         *                  {@link NsdHelper#ERROR_TYPE_STOP_DISCOVERY_FAILED}
         *                  {@link NsdHelper#ERROR_TYPE_RESOLVE_FAILED}
         * @param errorCode can be one of
         *                  {@link NsdManager#FAILURE_ALREADY_ACTIVE}
         *                  {@link NsdManager#FAILURE_INTERNAL_ERROR}
         *                  {@link NsdManager#FAILURE_MAX_LIMIT}
         */
        void onError(int errorType, int errorCode);
    }

    /**
     * Callback interface which is invoked when a local
     * service is found or lost after calling
     * {@link NsdHelper#discover(String deviceType)}.
     */
    public interface ServiceListener {
        /**
         * Called when a local service is found.
         * @param serviceInfo Information of found service. Note that this
         *                    is an unresolved {@link NsdServiceInfo} object.
         *                    Thus, it does not contain full information of the
         *                    service. To get the full information, use
         *                    {@link NsdHelper#resolve(NsdServiceInfo serviceInfo)}
         */
        void onServiceFound(NsdServiceInfo serviceInfo);

        /**
         * Called when a local service is lost.
         * @param serviceInfo service which is lost. use
         * {@link NsdServiceInfo#getServiceName()} to compare
         * the lost service with other services.
         */
        void onServiceLost(NsdServiceInfo serviceInfo);
    }

    /**
     * Callback interface which is invoked when a service is
     * resolved using {@link NsdHelper#resolve(NsdServiceInfo serviceInfo)}.
     */
    public interface ResolveListener {
        /**
         * Called when an {@link NsdServiceInfo} is resolved.
         * @param serviceInfo {@link NsdServiceInfo} which contains full
         *                    information about the service.
         */
        void onServiceResolved(NsdServiceInfo serviceInfo);
    }

    private class NsdRegistrationListener implements NsdManager.RegistrationListener {

        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, final int errorCode) {
            registering = false;
            registered = false;
            Log.w(TAG, "registration failed: " + getErrorMessage(errorCode), null);
            if (errorListener != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        errorListener.onError(ERROR_TYPE_REGISTRATION_FAILED, errorCode);
                    }
                });
            }
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, final int errorCode) {
            registering = false;
            Log.w(TAG, "unregistration failed: " + getErrorMessage(errorCode), null);
            if (errorListener != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        errorListener.onError(ERROR_TYPE_UNREGISTRATION_FAILED, errorCode);
                    }
                });
            }
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            registering = false;
            registered = true;
            name = serviceInfo.getServiceName();
            Log.d(TAG, "Name: " + serviceInfo.getServiceName());
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            registered = false;
            name = null;
            if (reregister && !registering) {
                reregister = false;
                register(NsdHelper.this.serviceInfo);
            }
        }
    }

    private class NsdDiscoveryListener implements NsdManager.DiscoveryListener {
        @Override
        public void onStartDiscoveryFailed(String serviceType, final int errorCode) {
            discoveryStarting = false;
            discovering = false;
            Log.w(TAG, "startDiscovery failed: " + getErrorMessage(errorCode), null);
            if (errorListener != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        errorListener.onError(ERROR_TYPE_START_DISCOVERY_FAILED, errorCode);
                    }
                });
            }
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, final int errorCode) {
            discoveryStarting = false;
            Log.w(TAG, "stopDiscovery failed: " + getErrorMessage(errorCode), null);
            if (errorListener != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        errorListener.onError(ERROR_TYPE_STOP_DISCOVERY_FAILED, errorCode);
                    }
                });
            }
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
            discoveryStarting = false;
            discovering = true;
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            discovering = false;
            if (rediscover && !discoveryStarting) {
                rediscover = false;
                discover(NsdHelper.this.serviceType);
            }
        }

        @Override
        public void onServiceFound(final NsdServiceInfo serviceInfo) {
            if (excludeOwnService && name != null && serviceInfo.getServiceName().equals(name)) {
                return;
            }
            Log.d(TAG, "Found:" + serviceInfo.getServiceName());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    services.add(serviceInfo);
                    if (serviceListener != null) {
                        serviceListener.onServiceFound(serviceInfo);
                    }
                }
            });
        }

        @Override
        public void onServiceLost(final NsdServiceInfo serviceInfo) {
            Log.d("NsdHelper", "Lost: " + serviceInfo.getServiceName());
            int i = 0;
            boolean found = false;
            for (NsdServiceInfo nsdServiceInfo : services) {
                if (nsdServiceInfo.getServiceName().equals(serviceInfo.getServiceName())) {
                    found = true;
                    break;
                }
                i++;
            }

            final boolean finalFound = found;
            final int finalI = i;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (finalFound) {
                        services.remove(finalI);
                    }

                    if (serviceListener != null) {
                        serviceListener.onServiceLost(serviceInfo);
                    }
                }
            });
        }
    }

    private class NsdResolveListener implements NsdManager.ResolveListener {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, final int errorCode) {
            Log.w(TAG, "resolve failed: " + getErrorMessage(errorCode), null);
            if (errorListener != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        errorListener.onError(ERROR_TYPE_RESOLVE_FAILED, errorCode);
                    }
                });
            }
        }

        @Override
        public void onServiceResolved(final NsdServiceInfo serviceInfo) {
            if (resolveListener != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resolveListener.onServiceResolved(serviceInfo);
                    }
                });
            }
        }
    }
}