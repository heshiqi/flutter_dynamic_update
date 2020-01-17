import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dynamic_update/dynamic_update.dart';

void main() {
  const MethodChannel channel = MethodChannel('dynamic_update');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await DynamicUpdate.platformVersion, '42');
  });
}
