# 🎼 EMA - Escola de Música de Anchieta: Protótipo de Aplicativo Interativo

## Visão Geral do Projeto

Este é um protótipo de aplicativo Android desenvolvido para a Escola de Música de Anchieta (EMA), com foco em adolescentes de 12 a 17 anos. O objetivo principal é criar uma plataforma interativa que motive e engaje os alunos em seus estudos musicais, promova o acompanhamento de progresso, o compartilhamento de performances e a colaboração social. O projeto busca alinhar o desenvolvimento tecnológico com os princípios da Aprendizagem Baseada em Projetos (ABP), enfatizando o "aprender fazendo" e a "construção colaborativa de conhecimentos".

## Funcionalidades Principais

O protótipo atual oferece as seguintes funcionalidades essenciais:

* **Autenticação de Usuários:**
    * **Cadastro e Login:** Permite que novos usuários se registrem e usuários existentes façam login utilizando e-mail e senha, gerenciados pelo Firebase Authentication.
    * **Confirmação de Saída:** Exibe um `AlertDialog` de confirmação ao pressionar o botão "Voltar" nas telas principais para evitar saídas acidentais.
* **Gestão de Perfil do Aluno:**
    * **Coleta de Dados Adicionais:** Após o cadastro, o aluno é direcionado para uma tela para preencher informações de perfil (nome completo, celular, instrumento, idade, endereço), que são persistidas no Cloud Firestore.
    * **Edição de Perfil:** O aluno pode editar seus próprios dados de perfil a qualquer momento, acessando a opção "Editar Perfil" via menu de overflow (três pontos) na `Toolbar` do Dashboard.
* **Dashboard Principal:**
    * Um "hub" central para as atividades do aluno.
    * **Mensagens Motivacionais Dinâmicas:** Exibe mensagens de status personalizadas que mudam com base no tempo de estudo do dia do aluno (ex: "Vamos treinar?", "Ótimo começo!", "Você está estudando!").
* **Acompanhamento de Progresso e Gamificação (Simplificado):**
    * **Contador de Tempo de Estudo:** Botões "Iniciar Estudo" e "Parar Estudo" com um cronômetro em tempo real (`HH:MM:SS`).
    * **Persistência Diária:** O tempo de estudo é salvo localmente (`SharedPreferences`) e sincronizado com o Cloud Firestore diariamente.
    * **Histórico de Estudo:** Exibe um histórico rolável dos tempos de estudo dos últimos dias (`RecyclerView`), permitindo que o aluno visualize seu progresso ao longo do tempo.
    * **Barra de Progresso:** Um elemento visual simples de progresso com base nas metas de tempo de estudo.
* **Compartilhamento de Performance (Simplificado):**
    * Permite ao usuário "publicar" performances com um título e um link de vídeo (ex: YouTube).
    * Exibe uma lista simples das performances publicadas por outros usuários.
* **Módulo de Colaboração (Simplificado):**
    * **Grupos de Estudo:** Exibe uma lista de grupos existentes onde o aluno pode "entrar" (visual).
    * **Chat Simples em Grupo:** Permite que os membros do grupo postem mensagens, que são salvas e carregadas do Cloud Firestore.
    * **Nomes dos Remetentes:** As mensagens exibem o nome completo do remetente (puxado do perfil do Firestore) em vez do e-mail.

## Tecnologias e Ferramentas Utilizadas

Este protótipo foi construído com base em uma stack moderna e robusta para desenvolvimento Android nativo, seguindo as melhores práticas de arquitetura e design.

* **Linguagem de Programação:**
    * **Kotlin:** Linguagem oficial e preferencial para o desenvolvimento Android.
* **Arquitetura:**
    * **MVVM (Model-View-ViewModel):** Padrão arquitetural para separação de responsabilidades, com `Model` (dados), `View` (UI em `screen` package) e `ViewModel` (lógica de UI).
* **Desenvolvimento da Interface (UI):**
    * **XML:** Definição de layouts.
    * **Material Design:** Princípios e componentes visuais para UI, alinhados à identidade da EMA.
    * **ViewBinding:** Acesso seguro aos elementos da UI.
    * **RecyclerView, CardView, NestedScrollView:** Componentes para exibição eficiente de listas e conteúdo rolável.
    * **Toolbar (AppCompat):** Barra de aplicativo customizada com logo e menu.
* **Gerenciamento de Dependências:**
    * **Gradle Kotlin DSL (KTS):** Configuração de build.
    * **Gradle Versions Catalog (`libs.versions.toml`):** Centralização e organização de versões de bibliotecas.
* **Persistência e Backend:**
    * **Firebase Authentication (E-mail/Senha):** Autenticação de usuários.
    * **Cloud Firestore:** Banco de dados NoSQL para dados de perfil, logs de estudo e mensagens de grupo.
    * **Regras de Segurança do Firestore:** Controle de acesso aos dados.
    * **`google-services.json`:** Conexão entre o app e o Firebase.
    * **`SharedPreferences`:** Cache local para dados de estudo (offline-first).
* **Outros Recursos e Boas Práticas:**
    * **LiveData:** Observação reativa de dados.
    * **Clean Code e Princípios SOLID:** Padrões para código limpo e manutenível.

## Estrutura do Projeto (Principais Pacotes)
```
├── .gradle/                                   # Diretórios de build do Gradle
├── .idea/                                     # Arquivos de configuração do Android Studio
├── app/                                       # Módulo principal da aplicação Android
│   ├── build.gradle.kts                       # Script de build do módulo 'app'
│   ├── google-services.json                   # Arquivo de configuração do Firebase (na raiz do módulo 'app')
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/
│           │   └── com/
│           │       └── ema/
│           │           └── musicschool/
│           │               ├── data/          # Modelos de dados e LocalDataSource
│           │               ├── screen/        # Activities e Fragments (UI)
│           │               └── viewmodels/    # ViewModels para lógica de UI
│           └── res/                           # Recursos do Android
│               ├── drawable/
│               ├── layout/
│               ├── mipmap/
│               ├── menu/                      # dashboard_menu.xml
│               └── values/
├── build.gradle.kts                           # Script de build do projeto (nível raiz)
├── libs.versions.toml                         # Catálogo de versões do Gradle (na raiz do projeto)
└── settings.gradle.kts                        # Configurações do projeto Gradle
```
