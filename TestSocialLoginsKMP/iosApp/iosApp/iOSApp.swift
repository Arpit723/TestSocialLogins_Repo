import SwiftUI
import FirebaseCore
import GoogleSignIn
import Shared

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        GoogleSignInBridgeHolder.shared.bridge = GoogleSignInBridgeImpl()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
