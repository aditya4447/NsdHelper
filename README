# NsdHelper

Android library to regsiter and discover local network services (dns-sd). 

## Installation

Enter the following line as a dependency in your app-level build.gradle:

```gradle
implementation ''
```

## Usage

### Create an NsdHelper object:

```java
public class MainActivity extends AppCompatActivity {
    ...
    private NsdHelper nsdHelper;
    ...
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ...
        nsdHelper = new NsdHelper(this);
        ...
    }
}
```

### Registering a service

Create an [NsdServiceInfo](https://developer.android.com/reference/android/net/nsd/NsdServiceInfo) object and set service name and service type as described in [http://www.dns-sd.org/](http://www.dns-sd.org/).

```java
NsdServiceInfo serviceInfo = new NsdServiceInfo();
serviceInfo.setServiceType("_myProtocol._tcp");
serviceInfo.setServiceName("_MyService");
serviceInfo.setPort(...);
```

Aditionally, for api level 19 and above, you can also add additional attributes:

```java
serviceInfo.setAttribute("MyUserName", "god");
serviceInfo.setAttribute("MyDescription", "My new dns sd service");
```

Finaly, call register(serviceInfo) :

```java
nsdHelper.register(serviceInfo);
```

### Discovering services

Register callbacks to get notified when a service is discovered or lost:

```java
nsdHelper.setServiceListener(new NsdHelper.ServiceListener() {
    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {
        String serviceName = serviceInfo.getServiceName();
        ...
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
        String serviceName = serviceInfo.getServiceName()
        ...
    }
});
```

Finally, call discover(serviceType) :

```java
nsdHelper.discover("_myProtocol._tcp");
```

### Resolving service

NsdServiceInfo object passed in NsdHelper.ServiceListener are unresolved and doesnot contain full information about the services we found. To get full information about a service, we have to resolve it. For that, register a callback:

```java
nsdHelper.setResolveListener(new NsdHelper.ResolveListener() {
    @Override
    public void onServiceResolved(NsdServiceInfo serviceInfo) {
        String name = serviceInfo.getServiceName();
        int port = serviceInfo.getPort();
        // For api level >= 19
        // String username = serviceInfo.getAttributes().get("MyUserName");
        // String description = serviceInfo.getAttributes().get("MyDescription");
    }
});
```

Finally, call resolve(serviceInfo) :

```java
nsdHelper.resolve(serviceInfo);
```

### Handling errors

You can register a callback to get notified when an error occurs:

```java
nsdHelper.setErrorListener(new NsdHelper.NsdErrorListener() {
    @Override
    public void onError(int errorType, int errorCode) {
        Toast.makeText(MainActivity.this, NsdHelper.getErrorMessage(errorCode),
                Toast.LENGTH_LONG).show();
    }
});
```

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
Copyright 2020 Aditya Nathwani

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this project except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.