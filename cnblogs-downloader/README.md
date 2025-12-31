# 博客园文章图片下载工具

这是一个用于下载博客园文章正文中所有图片的Python工具。

## 功能特点

- 自动访问指定博客园文章URL
- 提取文章标题作为下载目录名
- 识别文章正文中的所有图片
- 自动下载图片到本地
- 按"文章名_序号"格式命名图片（从0开始）

## 安装依赖

```bash
pip install -r requirements.txt
```

## 使用方法

运行脚本时，将博客园文章URL作为命令行参数传入：

```bash
python download_cnblogs_images.py https://www.cnblogs.com/wintersun/p/19390629
```

或者查看帮助信息：

```bash
python download_cnblogs_images.py --help
```

## 输出说明

- 程序会在当前目录下创建一个以文章标题命名的文件夹
- 所有图片将保存在该文件夹中
- 图片命名格式：`文章名_0.jpg`、`文章名_1.jpg` 等

## 注意事项

- 程序会自动跳过头像、图标等非内容图片
- 如果图片URL没有扩展名，程序会尝试从HTTP响应头推断
- 下载过程中会有适当的延迟，避免请求过快
