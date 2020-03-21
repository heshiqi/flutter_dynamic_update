import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

void attention() => runApp(AttentionPage());

class AttentionPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      title: '关注',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home:Scaffold(
        appBar: AppBar(
          centerTitle: true,
          // Here we take the value from the MyHomePage object that was created by
          // the App.build method, and use it to set our appbar title.
          title: Text("关注页面"),
        ),
        body: Container(
          child: Text("关注页面\n关注列表"),
        ),
      )
    );
  }
}
