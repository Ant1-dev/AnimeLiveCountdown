<div align="center">
  <img src="./animeCountdownApp/public/icons/favicon.png" alt="Anime Live Countdown Logo" height="100px">

# Anime Live Countdown

**A modern web application to track upcoming seasonal anime with real-time countdowns**

[![Angular](https://img.shields.io/badge/Angular-20.3-DD0031?logo=angular)](https://angular.dev/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-latest-4169E1?logo=postgresql)](https://www.postgresql.org/)
[![AWS](https://img.shields.io/badge/AWS-Deployed-FF9900?logo=amazonwebservices)](https://aws.amazon.com/)
[![Vercel](https://img.shields.io/badge/Vercel-Deployed-000000?logo=vercel)](https://vercel.com/)

</div>

---

## 📖 About The Project

<div align="center">
  <img src="./animeCountdownApp/public/images/preview-image.jpg" alt="Project Preview" width="100%">
</div>

<br>

Anime Live Countdown is a user-friendly web application designed to help anime fans track upcoming shows in the current season. What sets this platform apart is its unique **day-of-the-week grouping system**, which gives countdown timers meaningful context by showing when each anime airs throughout the week.

### ✨ Key Features

- 📅 **Weekly Schedule View** - Shows organized by day of the week for easy planning
- ⏱️ **Live Countdowns** - Real-time timers for upcoming episode releases
- 🔥 **Trending Sidebar** - Top 10 airing anime with quick access
- 🔍 **Advanced Search** - Search and filter anime from the database
- 🎭 **Detailed Media Pages** - Comprehensive information about each anime
- 👤 **User Authentication** - Secure sign-in with user profiles
- ⭐ **Favorites System** - Save and track your favorite shows
- 📱 **Responsive Design** - Optimized for mobile, tablet, and desktop
- 🎨 **Modern UI** - Clean, modern design with smooth animations

---

## 🛠️ Built With

### Frontend

- **[Angular 20.3](https://angular.dev/)** - Modern web framework with standalone components
- **[RxJS 7.8](https://rxjs.dev/)** - Reactive programming with signals
- **[TypeScript 5.9](https://www.typescriptlang.org/)** - Type-safe JavaScript

### Backend

- **[Spring Boot 3.4](https://spring.io/projects/spring-boot)** - Java backend framework
- **[Spring Security](https://spring.io/projects/spring-security)** - Authentication & authorization
- **[Spring Data JPA](https://spring.io/projects/spring-data-jpa)** - Database abstraction layer
- **[PostgreSQL](https://www.postgresql.org/)** - Relational database
- **[Maven](https://maven.apache.org/)** - Dependency management

### Infrastructure & Deployment

- **[Vercel](https://vercel.com/)** - Frontend hosting
- **[AWS](https://aws.amazon.com/)** - Backend hosting and services
- **[Docker](https://www.docker.com/)** - Containerization

---

## 📁 Project Structure

```
AnimeLiveCountdown/
├── animeCountdownApp/          # Angular Frontend Application
│   ├── src/
│   │   ├── app/
│   │   │   ├── auth/           # Authentication components
│   │   │   ├── favorites/      # User favorites page
│   │   │   ├── footer/         # Footer component
│   │   │   ├── header/         # Navigation & search
│   │   │   │   ├── searchbar/  # Search functionality
│   │   │   │   ├── sign-in/    # Sign-in modal
│   │   │   │   └── profile-dropdown/
│   │   │   ├── home/           # Main landing page
│   │   │   │   ├── hero-carousel/      # Hero banner carousel
│   │   │   │   └── weekly-schedule/
│   │   │   │       ├── airing-soon/    # Upcoming releases
│   │   │   │       ├── shows/          # Weekly schedule grid
│   │   │   │       └── season-pop/     # Season popularity section
│   │   │   ├── media-info/     # Detailed anime pages
│   │   │   ├── shared-components/      # Reusable components
│   │   │   │   └── sidebar/    # Trending anime sidebar
│   │   │   ├── models/         # TypeScript interfaces
│   │   │   ├── services/       # API & business logic
│   │   │   └── interceptors/   # HTTP interceptors
│   │   └── public/             # Static assets
│   ├── vercel.json             # Vercel deployment config
│   ├── package.json            # Frontend dependencies
│   └── tsconfig.json           # TypeScript configuration
│
├── animeCountdownBackend/      # Spring Boot Backend API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/           # Java source code
│   │   │   └── resources/      # Application configs
│   │   └── test/               # Backend tests
│   ├── Dockerfile              # Backend containerization
│   ├── docker-compose.yml      # Local development setup
│   └── pom.xml                 # Maven dependencies
│
├── .github/                    # GitHub workflows & configs
├── LICENSE                     # MIT License
└── README.md                   # This file
```


---

## 🗺️ Roadmap

### ✅ Completed Features

- [x] **Search Functionality**
  - [x] Real-time search bar with autocomplete
  - [x] Database filtering and query optimization
  - [x] Detailed media information pages
- [x] **User Authentication**
  - [x] Secure sign-in/sign-up system
  - [x] JWT-based authentication
  - [x] User profile management
- [x] **Weekly Schedule**
  - [x] Day-of-week organization
  - [x] Live countdown timers
  - [x] Airing soon section
  - [x] Hero carousel for featured shows
  - [x] Season popularity rankings
- [x] **Trending Sidebar**
  - [x] Top 10 currently airing anime
  - [x] Live episode countdowns
  - [x] Click-to-navigate functionality
  - [x] Responsive design (hidden on mobile)
- [x] **Favorites System**
  - [x] Save favorite anime
  - [x] Personal favorites page
- [x] **Responsive Design**
  - [x] Mobile-first approach
  - [x] Dynamic navbar (hides on scroll down, shows on scroll up)
  - [x] Adaptive grid layouts for all screen sizes
  - [x] Optimized breakpoints (mobile, tablet, desktop)

### 🚧 In Progress

- [ ] **Push Notifications**
  - [ ] Email notifications for new episodes
  - [ ] Browser push notification support
  - [ ] Customizable notification preferences

### 🔮 Future Enhancements

- [ ] **Advanced Filtering**
  - [ ] Genre-based filters
  - [ ] Studio and producer filters
  - [ ] Seasonal archive browsing
  - [ ] English dub availability filter
- [ ] **Personalization**
  - [ ] Theme customization (dark/light mode)
  - [ ] Customizable dashboard layouts
  - [ ] Watch history tracking
- [ ] **Social Features**
  - [ ] User reviews and ratings
  - [ ] Episode tracking
  - [ ] Community discussion boards

---

## 📄 License

Distributed under the MIT License. See [LICENSE](LICENSE) for more information.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the issues page.

---

<div align="center">
  Made with ❤️ for the anime community
</div>
