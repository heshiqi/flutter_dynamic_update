import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class Publish extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      title: '发布页面',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home:Scaffold(
        appBar: AppBar(
          centerTitle: true,
          // Here we take the value from the MyHomePage object that was created by
          // the App.build method, and use it to set our appbar title.
          title: Text("发布页面"),
        ),
        body: Container(
          child: Text("发布页面\n发布页面"),
        ),
      )
    );
  }
}
