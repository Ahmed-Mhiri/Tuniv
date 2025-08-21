```mermaid
graph TD
    subgraph GlobalNav["Global Navigation"]
        G1[Search Bar]
        G2[Ask Question Button]
        G3[Notifications Icon]
        G4[<b>Messages Icon</b>]
        G5[User Profile Dropdown]
    end

    subgraph Pages["Main Application Pages"]
        A(Personalized Dashboard)
        QAP[Q&A Page]
        BDP[Browse & Discover Pages]
        BDP --> P1[Universities Page] & P3[Modules Page]
        UP[<b>User Profile Page (Detailed)</b>]
        UP --> U1[Photo, Bio, Major, Reputation]
        UP --> U2[Role at University (e.g., Student @ INSAT)]
        UP --> U3[Tabs: Questions, Answers, Badges]
        MP[<b>Direct Messaging Page</b>]
        MP --> MP1[List of Conversations]
        MP --> MP2[Active Chat Window]
    end

    subgraph AdminPanel["Admin Panel"]
        style AdminPanel fill:#fff3e0,stroke:#333
        AP1[User & Content Management]
    end