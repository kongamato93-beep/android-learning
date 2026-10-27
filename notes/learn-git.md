##git和.md如何使用

# git如何使用

## 1.全局配置
```bash
ASUS@LAPTOP-HCVVAPPH UCRT64 /d/andriod/WeChat
$ git config --global user.name "kongamato"

ASUS@LAPTOP-HCVVAPPH UCRT64 /d/andriod/WeChat
$ git config --global user.email "kongamato93@gmail.com"

ASUS@LAPTOP-HCVVAPPH UCRT64 /d/andriod/WeChat
$ git config --global --list
safe.directory=*
user.name=kongamato
user.email=kongamato93@gmail.com
```
## 2.第一次上传
```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/用户名/仓库名.git
git push -u origin main
```
每条命令的作用：
| 指令	| 作用 |
| --- | --- |
git init	| 初始化本地 Git 仓库
git add .	|选中要纳入版本记录的文件
git commit	|保存第一个版本
git remote add origin	|记录 GitHub 仓库地址
git push -u origin main	|首次上传并设置分支跟踪关系

以后不需要反复 git init 或 git remote add origin。

## 3.日常开发一个功能
确认当前状态:git status -> 开发功能 -> 检查代码改动：git status 和 git diff -> 运行和测试 -> 保存一个版本 git add . 和 git commit -m "注释改动内容" -> 上传到github：git push





# .md文件的排版如何使用
## 1.标题和分层
用#来分层
## 2.代码块
```bash(指定语言)
#代码
```
## 3.表格
常见语言标记：
| 语言 | 标记 |
| --- | --- |
C++	| cpp
Java	| java
Python	| python 
Shell命令 | bash 
JSON	|json
SQL	|sql
普通文本	|text

## 4.代办清单
- [x] 解决问题1
- [ ] 解决问题2
## 5.插入
[手册](路径)
![图片](路径)
## 6.字体
**重要内容**
*斜体内容*
`string`
~~错误内容~~
## 7.列表
无序
- 学习 C++ 字符串
- 完成 PAT 题目
- 整理代码

有序
1. 读取输入
2. 计算商数和余数
3. 删除商数前导零
4. 输出结果

嵌套
- Linux
  - 文件操作
  - 用户权限
  - 进程管理
- 网络
  - TCP
  - DNS
  - HTTP

 
