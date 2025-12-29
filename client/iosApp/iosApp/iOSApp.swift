import SwiftUI
import ComposeApp

@main struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView().onOpenURL {
                url in
                // Handle deep links for OAuth callbacks
                // This forwards the URL to Supabase Auth for processing
                DeepLinkHandlerKt.handleDeepLink(url: url.absoluteString)
            }
        }
    }
}
