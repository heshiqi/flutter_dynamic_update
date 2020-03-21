import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';


void live() => runApp(LivePage());


class LivePage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      title: '直播',
      theme: ThemeData(
        primarySwatch: Colors.red,
      ),
      home:Scaffold(
        appBar: AppBar(
          centerTitle: true,
          // Here we take the value from the MyHomePage object that was created by
          // the App.build method, and use it to set our appbar title.
          title: Text("直播页面"),
        ),
        body: Container(
          child: Text("直播页面\n直播列表"),
        ),
      )
    );
  }
}
