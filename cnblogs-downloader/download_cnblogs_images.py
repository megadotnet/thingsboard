#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
博客园文章图片下载工具
访问指定博客园文章，提取正文中的所有图片并下载到本地
"""

import os
import re
import sys
import argparse
import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin, urlparse
import time


def sanitize_filename(filename):
    """清理文件名，移除非法字符"""
    # 移除Windows文件名中不允许的字符
    illegal_chars = r'[<>:"/\\|?*]'
    filename = re.sub(illegal_chars, '_', filename)
    # 移除首尾空格和点
    filename = filename.strip(' .')
    # 限制文件名长度
    if len(filename) > 200:
        filename = filename[:200]
    return filename


def get_article_content(url):
    """获取文章HTML内容"""
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }
    try:
        response = requests.get(url, headers=headers, timeout=30)
        response.raise_for_status()
        response.encoding = response.apparent_encoding
        return response.text
    except requests.RequestException as e:
        print(f"获取网页失败: {e}")
        return None


def extract_article_info(html):
    """从HTML中提取文章标题和正文内容"""
    soup = BeautifulSoup(html, 'html.parser')

    # 提取文章标题
    title = None
    # 尝试多种可能的标题选择器
    title_selectors = [
        'h1.postTitle',
        'h1#cb_post_title_url',
        'h1.post-title',
        'h1',
        '.postTitle',
        '#cb_post_title_url'
    ]

    for selector in title_selectors:
        title_elem = soup.select_one(selector)
        if title_elem:
            title = title_elem.get_text().strip()
            break

    if not title:
        # 如果找不到标题，尝试从页面标题提取
        title_tag = soup.find('title')
        if title_tag:
            title = title_tag.get_text().strip()
            # 移除博客园后缀
            title = re.sub(r'\s*-\s*博客园.*$', '', title)

    if not title:
        title = "未命名文章"

    # 提取文章正文
    # 博客园的文章正文通常在 #cnblogs_post_body 或 .postBody 中
    content_selectors = [
        '#cnblogs_post_body',
        '.postBody',
        '#post_body',
        '.post-body',
        'div#cnblogs_post_body'
    ]

    content = None
    for selector in content_selectors:
        content_elem = soup.select_one(selector)
        if content_elem:
            content = content_elem
            break

    if not content:
        print("警告: 未找到文章正文区域，将搜索整个页面的图片")
        content = soup

    return title, content


def extract_images(content, base_url):
    """从文章内容中提取所有图片URL"""
    images = []

    # 查找所有img标签
    img_tags = content.find_all('img')

    for img in img_tags:
        # 获取图片URL
        img_url = img.get('src') or img.get('data-src') or img.get('data-original')

        if not img_url:
            continue

        # 处理相对URL
        img_url = urljoin(base_url, img_url)

        # 跳过一些常见的非内容图片（如头像、图标等）
        skip_patterns = [
            r'avatar',
            r'icon',
            r'logo',
            r'button',
            r'thumb_thumb',  # 博客园常见的缩略图
        ]

        should_skip = False
        for pattern in skip_patterns:
            if re.search(pattern, img_url, re.IGNORECASE):
                should_skip = True
                break

        if not should_skip:
            images.append(img_url)

    return images


def download_image(url, filepath):
    """下载图片到指定路径"""
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
        'Referer': 'https://www.cnblogs.com/'
    }

    try:
        response = requests.get(url, headers=headers, timeout=30, stream=True)
        response.raise_for_status()

        # 获取文件扩展名
        parsed_url = urlparse(url)
        path = parsed_url.path
        ext = os.path.splitext(path)[1]

        # 如果没有扩展名，尝试从Content-Type获取
        if not ext:
            content_type = response.headers.get('Content-Type', '')
            if 'jpeg' in content_type or 'jpg' in content_type:
                ext = '.jpg'
            elif 'png' in content_type:
                ext = '.png'
            elif 'gif' in content_type:
                ext = '.gif'
            elif 'webp' in content_type:
                ext = '.webp'
            else:
                ext = '.jpg'  # 默认使用jpg

        # 确保文件路径有正确的扩展名
        if not filepath.endswith(ext):
            filepath = filepath + ext

        # 下载并保存图片
        with open(filepath, 'wb') as f:
            for chunk in response.iter_content(chunk_size=8192):
                if chunk:
                    f.write(chunk)

        return True, filepath
    except Exception as e:
        print(f"下载图片失败 {url}: {e}")
        return False, None


def main():
    """主函数"""
    # 解析命令行参数
    parser = argparse.ArgumentParser(
        description='博客园文章图片下载工具',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='示例:\n  python download_cnblogs_images.py https://www.cnblogs.com/wintersun/p/19390629'
    )
    parser.add_argument(
        'url',
        type=str,
        help='博客园文章URL'
    )

    args = parser.parse_args()
    url = args.url

    # 验证URL格式
    if not url.startswith('http://') and not url.startswith('https://'):
        print(f"错误: URL格式不正确，应以http://或https://开头")
        sys.exit(1)

    print(f"正在访问: {url}")

    # 获取HTML内容
    html = get_article_content(url)
    if not html:
        print("无法获取网页内容，程序退出")
        return

    # 提取文章信息
    print("正在解析文章内容...")
    title, content = extract_article_info(html)
    print(f"文章标题: {title}")

    # 清理标题作为目录名
    dir_name = sanitize_filename(title)

    # 创建目录
    if not os.path.exists(dir_name):
        os.makedirs(dir_name)
        print(f"创建目录: {dir_name}")
    else:
        print(f"目录已存在: {dir_name}")

    # 提取图片URL
    print("正在提取图片...")
    images = extract_images(content, url)
    print(f"找到 {len(images)} 张图片")

    if not images:
        print("未找到图片，程序退出")
        return

    # 下载图片
    print("\n开始下载图片...")
    success_count = 0
    for idx, img_url in enumerate(images):
        # 生成文件名: 文章名_序号
        filename = f"{dir_name}_{idx}"
        filepath = os.path.join(dir_name, filename)

        print(f"[{idx + 1}/{len(images)}] 下载: {img_url}")
        success, saved_path = download_image(img_url, filepath)

        if success:
            print(f"  ✓ 保存成功: {saved_path}")
            success_count += 1
        else:
            print(f"  ✗ 下载失败")

        # 避免请求过快
        time.sleep(0.5)

    print(f"\n下载完成! 成功: {success_count}/{len(images)}")
    print(f"图片保存在目录: {dir_name}")


if __name__ == "__main__":
    main()
