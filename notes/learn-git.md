### 10月22日学习日志

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
## 2.基础命令
### 1)新建本地仓库
git init
### 2）查看文件改动改成
git status
红色：改动了但还没放进暂存区
绿色：
加入暂存区：git add 文件名
将所有文件加入缓存区：git add .
将暂存区中的内容保存为一个版本(提交至本地仓库)：git commit -m"备注：内容"(添加备注)
保存到远程仓库：git push
## 3.绑定远程仓库
git remote add origin 仓库地址


ASUS@LAPTOP-HCVVAPPH UCRT64 /d/andriod/WeChat (main)
$ git commit .
On branch main
nothing to commit, working tree clean

ASUS@LAPTOP-HCVVAPPH UCRT64 /d/andriod/WeChat (main)
$ git 



# .md文件的排版如何使用
## 1.标题和分层
用#来分层
## 2.代码块
```bash
#代码
```
## 3.代办清单
- [x] 解决问题1
- [ ] 解决问题2
## 4.插入
[手册](路径)
![图片](路径)
