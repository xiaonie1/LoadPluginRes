# LoadPluginRes
通过ClassLoader,反射获取插件app中的资源展示
使用了PathClassLoader去加载已安装的插件app,通过相同的sharedUserId,去识别自己的插件
使用DexClassLoader加载未安装的app
