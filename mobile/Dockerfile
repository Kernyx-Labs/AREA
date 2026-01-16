FROM ghcr.io/cirruslabs/flutter:stable

WORKDIR /app

COPY pubspec.* ./

RUN flutter pub get

COPY . .

RUN flutter build apk --release

CMD ["cp", "build/app/outputs/flutter-apk/app-release.apk", "/shared/client.apk"]
