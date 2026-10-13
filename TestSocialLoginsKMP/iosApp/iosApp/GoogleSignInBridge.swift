import FirebaseCore
import GoogleSignIn
import Shared

final class GoogleSignInBridgeImpl: GoogleSignInBridge {

    func signIn(onSuccess: @escaping (GoogleSignInTokens) -> Void, onError: @escaping (String) -> Void) {
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            onError("Firebase client ID missing; check GoogleService-Info.plist")
            return
        }
        let config = GIDConfiguration(clientID: clientID)
        guard let presenting = topViewController() else {
            onError("No presenting view controller available")
            return
        }
        GIDSignIn.sharedInstance.configuration = config
        GIDSignIn.sharedInstance.signIn(withPresenting: presenting) { signInResult, error in
            if let error {
                onError(error.localizedDescription)
                return
            }
            guard let user = signInResult?.user, let idToken = user.idToken?.tokenString else {
                onError("Sign-in completed without an ID token")
                return
            }
            onSuccess(GoogleSignInTokens(idToken: idToken, accessToken: user.accessToken.tokenString))
        }
    }

    func signOut() {
        GIDSignIn.sharedInstance.signOut()
    }

    private func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        for scene in scenes {
            if let root = scene.windows.first(where: \.isKeyWindow)?.rootViewController {
                return topMost(from: root)
            }
        }
        return nil
    }

    private func topMost(from viewController: UIViewController) -> UIViewController {
        var current = viewController
        while let presented = current.presentedViewController {
            current = presented
        }
        return current
    }
}
