# Project Tracker (Android)

বাংলা-সহজ Personal Project & Daily Task Tracker — Android Studio source project.

## Features
- প্রজেক্ট যোগ ও ডিলিট
- প্রতিটি প্রজেক্টে দৈনিক টপিক/কাজ যোগ
- তারিখ অনুযায়ী Completed / Pending / Missed অবস্থা
- কাজ সম্পন্ন করলে সবুজ, নির্ধারিত তারিখ পেরিয়ে অসম্পন্ন থাকলে লাল
- প্রজেক্টভিত্তিক অগ্রগতি
- Room database: সব ডেটা ফোনের ভেতরে থাকে; ইন্টারনেট/অ্যাকাউন্ট লাগে না

## Build
1. Android Studio (Ladybug বা নতুন) ইনস্টল করুন।
2. ZIP extract করে `ProjectTracker` ফোল্ডার Android Studio-তে Open করুন।
3. Gradle sync শেষ হলে Run চাপুন, অথবা Build > Build Bundle(s) / APK(s) > Build APK(s)।
4. APK সাধারণত `app/build/outputs/apk/debug/app-debug.apk`-এ তৈরি হবে।

## Notes
- Minimum Android: 8.0 (API 26).
- তারিখ `YYYY-MM-DD` ফরম্যাটে লিখতে হবে।
- কোনো প্রজেক্ট ডিলিট করলে তার অধীনে থাকা কাজও ডিলিট হবে।
- এটি একটি MVP starter project; APK এই প্যাকেজে আগে থেকে build করা নেই.
