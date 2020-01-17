import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_dynamic_update/publish.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:permission_handler/permission_handler.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int _counter = 0;
  static const servicePlugin = const MethodChannel('dynamic_update');
  final PermissionGroup _permissionGroup = Platform.isIOS ? PermissionGroup.photos : PermissionGroup.storage;
  void _incrementCounter() {
    setState(() {
      _counter++;
    });
//    Navigator.push(context, MaterialPageRoute(builder: (context) {
//      return Publish();
//    })
//    );
  }

  Future<String> _dynamicUpdate() async{
    String value;
    try {
      value = await servicePlugin.invokeMethod("dynamicUpdate");
    }catch(e){
      print(e);
    }
    return value;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Text(
              'You have pushed the button this many times:',
            ),
            Text(
              '$_counter',
              style: Theme.of(context).textTheme.display1,
            ),
            FlatButton(
              color: Color(0x144183ff),
              padding: EdgeInsets.all(5),
              shape: RoundedRectangleBorder(
                side:
                new BorderSide(color: Color(0xFF2873FF), width: 0.5),
                borderRadius: BorderRadius.all(Radius.elliptical(3, 3)),
              ),
              child: new Text("动态更新插件",
                style:
                new TextStyle(fontSize: 12, color: Color(0xFF2873FF)),
              ),
              onPressed: () {
                requestPermission(_permissionGroup);
              },
            ),
            FlatButton(
              color: Color(0x144183ff),
              padding: EdgeInsets.all(5),
              shape: RoundedRectangleBorder(
                side:
                new BorderSide(color: Color(0xFF2873FF), width: 0.5),
                borderRadius: BorderRadius.all(Radius.elliptical(3, 3)),
              ),
              child: new Text("点一点",
                style:
                new TextStyle(fontSize: 12, color: Color(0xFF2873FF)),
              ),
              onPressed: () {
                Fluttertoast.showToast(
                    msg: "请跟新插件点击发布按钮进入发布页面",
                    toastLength: Toast.LENGTH_SHORT,
                    gravity: ToastGravity.BOTTOM,
                    timeInSecForIos: 1,
                    backgroundColor:Colors.red,
                    textColor: Colors.white);
              },
            ),
//            Text(
//              '插件已更新 请点击发布按钮进入发布页:',
//            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _incrementCounter,
        tooltip: 'Increment',
        child: Icon(Icons.add),
      ), // This trailing comma makes auto-formatting nicer for build methods.
    );
  }


  Future<void> requestPermission(PermissionGroup permission) async {
    final List<PermissionGroup> permissions = <PermissionGroup>[permission];
    final Map<PermissionGroup, PermissionStatus> permissionRequestResult =
    await PermissionHandler().requestPermissions(permissions);
    PermissionStatus _permissionStatus = permissionRequestResult[permission];
    if (_permissionStatus == PermissionStatus.granted) {
      if(Platform.isIOS){
      }else{
        var result=await _dynamicUpdate();
        if(result!=null){
          Fluttertoast.showToast(
              msg: result,
              toastLength: Toast.LENGTH_SHORT,
              gravity: ToastGravity.CENTER,
              timeInSecForIos: 1,
              backgroundColor:Colors.blueAccent,
              textColor: Colors.white);
        }
      }
    }
  }

}
