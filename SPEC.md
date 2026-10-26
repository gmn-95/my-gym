# my-gym — Especificação do aplicativo Android

## 1. Objetivo

Desenvolva o **my-gym**, um aplicativo **nativo para Android**, destinado ao meu uso pessoal para organizar fichas de musculação, executar treinos e consultar o histórico em um calendário.

O app deve ser **simples, rápido, intuitivo e prioritariamente offline**. Não precisa de login, cadastro de usuários, servidor próprio, sincronização em nuvem ou publicação na Play Store.

**Entregável esperado:** projeto Android completo, com código-fonte, instruções para compilar e instalar um APK, e mecanismo para atualizar o APK **sem perder os dados já salvos**.

> **Instrução para o Codex:** implemente o projeto de fato, e não apenas uma arquitetura, protótipo ou descrição. Tome decisões razoáveis quando houver detalhes em aberto, documente-as no README e priorize a conclusão de um MVP funcional antes de adicionar recursos opcionais.

---

## 2. Tecnologias e diretrizes

- **Plataforma:** Android nativo.
- **Linguagem:** Kotlin.
- **Interface:** Jetpack Compose com Material 3.
- **Armazenamento:** SQLite por meio do Room.
- **Preferências simples:** DataStore, caso necessário.
- **Arquitetura:** MVVM com ViewModel e repositórios, sem complexidade desnecessária.
- **Integração HTTP:** Retrofit/OkHttp ou solução equivalente consolidada.
- **Mídia:** biblioteca compatível com Compose e exibição de imagens/GIFs (por exemplo, Coil, com os componentes necessários).
- **Build:** Gradle com Gradle Wrapper incluído no repositório.
- **Idiomas:** português brasileiro (`pt-BR`) e inglês (`en`), com recursos de texto externalizados.
- **Tema:** claro, escuro e, preferencialmente, opção de seguir o sistema.
- **Foco:** aplicativo para um único usuário, sem telemetria ou anúncios.

Utilize versões estáveis e compatíveis das dependências. Escolha uma versão mínima do Android adequada ao projeto e documente a decisão no README.

---

## 3. Requisitos funcionais

### 3.1. Fichas / grupos de treino

O usuário deve poder:

- Criar **quantas fichas desejar**, sem um limite arbitrário no app.
- Atribuir nome livre a cada ficha, como `A — Superiores`, `B — Inferiores` ou `Treino de sábado`.
- Editar o nome e as configurações das fichas.
- Excluir fichas, após confirmação.
- Duplicar fichas (recurso simples e desejável).
- Reordenar os exercícios dentro de cada ficha.
- Ver uma lista de todas as fichas cadastradas.
- Abrir uma ficha para consultar seus exercícios e iniciar o treino.

Uma ficha é um **modelo de treino**. Alterar ou excluir esse modelo **não deve modificar sessões já realizadas**.

### 3.2. Exercícios de cada ficha

Cada ficha pode ter **N exercícios**. Para cada exercício, permitir:

- Adicionar um exercício escolhido no catálogo ou cadastrado manualmente.
- Remover o exercício da ficha sem excluí-lo de outras fichas.
- Configurar a quantidade de séries.
- Configurar a quantidade de repetições por série (valor fixo ou faixa, por exemplo, `8–12`).
- Configurar o tempo de descanso em segundos ou minutos.
- Editar esses dados posteriormente.
- Consultar nome, instruções e demonstrações visuais, quando disponíveis.

O mesmo exercício pode aparecer em várias fichas com configurações diferentes.

### 3.3. Busca e catálogo de exercícios

- Incluir barra de pesquisa por nome.
- Integrar, quando permitido pelos termos de uso, a **versão gratuita da ExerciseDB**:
  - Documentação: https://oss.exercisedb.dev/docs
  - Base da API: https://oss.exercisedb.dev
- Exibir dados disponíveis, como nome do exercício, grupo muscular, equipamento, instruções e **GIF** de demonstração.
- Não presumir disponibilidade de vídeos MP4 na versão gratuita; GIFs são suficientes para o MVP.
- Tratar carregamento, erro de rede, respostas vazias e limite de requisições de forma amigável.
- Implementar buscas eficientes, paginação quando suportada e evitar chamadas desnecessárias.
- Exibir a atribuição exigida pelo provedor da API.
- **Verificar e respeitar os termos vigentes da API**, inclusive restrições de armazenamento ou cache de textos, metadados e mídias. Não baixar nem manter uma cópia persistente do catálogo da API sem autorização.
- Manter o funcionamento das **fichas pessoais já criadas** sem internet. Se a licença impedir salvar conteúdo da API, armazenar apenas informações pertencentes ao usuário, como nomes e configurações inseridos por ele, e buscar detalhes/mídias remotos somente quando disponíveis.
- Permitir **cadastrar exercícios personalizados manualmente**, sem dependência da API. O cadastro manual deve permitir pelo menos nome e, opcionalmente, observações.

**Prioridade:** a ausência da API ou de conexão à internet **não pode impedir a abertura das fichas, o início dos treinos, o cronômetro ou o histórico**. Em caso de conflito entre acesso offline e licenciamento da API, priorize preservar os dados próprios do usuário e documente a limitação.

### 3.4. Início e acompanhamento do treino

Ao tocar em **Iniciar treino** em uma ficha:

1. Criar uma **sessão de treino** associada à ficha utilizada.
2. Iniciar automaticamente um **cronômetro de tempo total**, visível durante a execução.
3. Exibir todos os exercícios planejados, com séries, repetições e descanso configurados.
4. Permitir **marcar e desmarcar cada exercício como concluído**, sem alterar permanentemente a ficha-modelo.
5. Disponibilizar um temporizador regressivo de descanso para os exercícios, com ações **iniciar, pausar, continuar, reiniciar e encerrar**.
6. Permitir **pausar e retomar a sessão**, distinguindo essa pausa do temporizador de descanso.
7. Permitir **finalizar** ou **cancelar** a sessão, com confirmação apropriada.

Regras:

- O cronômetro da sessão deve mostrar o **tempo ativo**, descontando períodos em que a sessão ficou explicitamente pausada.
- O temporizador de descanso é **independente** do cronômetro total da sessão.
- Bloquear a tela ou alternar para outro app **não deve pausar automaticamente** a sessão ou o descanso.
- Cronômetros devem continuar corretos após a tela ser recriada e após o retorno do app do segundo plano; não depender de um contador que só avança enquanto a tela está aberta.
- Se o processo do app for encerrado, recuperar a sessão ativa e recalcular tempos a partir dos registros persistidos, tratando também reinicialização do aparelho de forma consistente.
- Evitar múltiplas sessões ativas simultaneamente na primeira versão; ao tentar iniciar outra, oferecer retomada ou finalização da sessão existente.
- Salvar o estado da sessão durante seu uso, para não perder marcações de exercícios e tempos em caso de encerramento inesperado.
- Uma sessão **concluída** deve permanecer no histórico. Uma sessão cancelada não deve aparecer como treino realizado no calendário.

**Escopo do MVP:** o registro obrigatório é a conclusão de cada **exercício**, não de cada série individual. Registro de carga e repetições efetivamente executadas por série pode ficar para uma versão futura.

### 3.5. Calendário e histórico

Implementar uma tela de calendário mensal que:

- Destaque os dias nos quais há ao menos uma sessão **concluída**.
- Permita selecionar um dia para listar as sessões daquele dia.
- Mostre o nome da ficha usada, horários/duração e exercícios concluídos ou não concluídos.
- Suporte **mais de um treino concluído no mesmo dia**.
- Permita abrir os detalhes de uma sessão antiga.
- Mantenha o histórico mesmo se uma ficha tiver sido renomeada, editada ou excluída.

Para preservar o histórico, salvar na sessão um **retrato (snapshot)** dos dados necessários da ficha e dos exercícios **pertencentes ao usuário**, respeitando as restrições de dados provenientes da API.

### 3.6. Duração e tempo médio

Cada ficha deve exibir uma indicação de duração:

- Quando **existirem sessões concluídas** daquela ficha, apresentar a **duração média real**, calculada com base no tempo ativo registrado nessas sessões.
- Se **não houver histórico**, mostrar `Sem histórico` ou, opcionalmente, uma **estimativa** calculada a partir de séries e descansos; qualquer estimativa deve estar identificada como tal.
- Não incluir sessões canceladas na média.
- A ficha deve permanecer identificável em registros históricos mesmo após edição; excluir a ficha não deve apagar durações históricas.

### 3.7. Temas e idiomas

- Suportar modo **claro** e **escuro**, com preferência persistida.
- Oferecer seleção entre **português brasileiro** e **inglês**, preferencialmente com opção de seguir o idioma do sistema.
- Traduzir todos os textos próprios do app: títulos, botões, menus, mensagens, erros e configurações.
- **Não traduzir automaticamente os nomes escritos pelo usuário**, nem prometer tradução de conteúdo da API caso ela não forneça tradução.
- Manter interface responsiva e legível em diferentes tamanhos de tela Android.

### 3.8. Exportação e importação em JSON

Criar ações acessíveis nas configurações:

- **Exportar** para arquivo `.json` por meio do seletor de arquivos/compartilhamento do Android.
- **Importar** um arquivo `.json` selecionado pelo usuário.
- Exportar **fichas, exercícios personalizados, configurações de cada exercício nas fichas, sessões e histórico**, além das preferências que fizer sentido restaurar.
- Adicionar um campo `schemaVersion` no JSON para permitir futuras evoluções do formato.
- Validar a estrutura antes da importação; mostrar erros claros para JSON inválido ou incompatível.
- Oferecer duas modalidades, se viável: **substituir os dados atuais** (mediante confirmação) ou **mesclar**, evitando duplicações de IDs. Caso a mesclagem não seja implementada no MVP, oferecer substituição com confirmação e documentar a limitação.
- Garantir que importações sejam **atômicas**: em caso de erro, não deixar o banco parcialmente restaurado.
- **Não exportar conteúdo da ExerciseDB de maneira incompatível com sua licença**. Dados de autoria do usuário podem integrar o backup.
- Não solicitar permissão de acesso irrestrito ao armazenamento; utilizar o **Storage Access Framework** do Android.

A exportação em JSON deve servir também como **backup manual antes de reinstalações ou alterações importantes**.

---

## 4. Persistência e modelo de dados

Usar Room/SQLite com entidades equivalentes às seguintes (pode ajustar a nomenclatura se justificar no README):

| Entidade | Responsabilidade |
|---|---|
| `WorkoutGroup` | Ficha de treino editável (nome, ordem, configurações gerais). |
| `WorkoutItem` | Exercício vinculado à ficha, sua posição, séries, repetições e descanso. |
| `CustomExercise` | Exercícios inseridos manualmente pelo usuário. |
| `WorkoutSession` | Sessão iniciada, pausada, concluída ou cancelada, com referência à ficha, horários e tempo ativo. |
| `SessionExercise` | Snapshot dos exercícios planejados e status de conclusão em cada sessão. |
| `AppPreferences` | Tema e idioma (pode ser DataStore, não precisa ser tabela). |

Requisitos de persistência:

- IDs estáveis e relacionamentos consistentes.
- Exclusão de fichas com confirmação e **sem exclusão em cascata do histórico**.
- Persistência de todos os dados necessários para continuar uma sessão interrompida.
- **Migrações explícitas do Room** quando o esquema evoluir; não utilizar `fallbackToDestructiveMigration()` em builds destinados ao uso pessoal.
- Não depender de dados de sessão guardados apenas na memória da tela.
- Salvar informações temporais de maneira consistente, evitando erros de fuso horário e mudança manual de relógio na medida do possível.

---

## 5. Telas esperadas

Desenvolver, no mínimo:

1. **Início / Meus treinos:** lista de fichas, duração média/estimada, ações para criar e acessar fichas.
2. **Criar / Editar ficha:** nome, lista e ordem dos exercícios, séries, repetições e descansos.
3. **Buscar exercício:** pesquisa, resultados e detalhe com demonstração e instruções quando disponíveis; opção de exercício manual.
4. **Treino em andamento:** cronômetro total, exercícios com marcação de conclusão, descanso com pausa/retomada e finalizar/cancelar.
5. **Calendário / Histórico:** dias treinados, sessões do dia e detalhes de cada sessão.
6. **Configurações:** idioma, tema, importação/exportação e créditos da fonte externa.

Priorizar navegação simples, botões fáceis de tocar durante a academia, boas mensagens de estado vazio e confirmação antes de ações destrutivas.

---

## 6. Atualização de APK sem perda de dados — requisito crítico

**Preciso conseguir instalar versões novas do app por cima da versão já instalada, preservando fichas, histórico, preferências e sessões persistidas.**

Implemente e documente:

1. Definir um **`applicationId` fixo e definitivo** desde a primeira versão. Não alterá-lo em atualizações.
2. Manter a **mesma chave/certificado de assinatura** em todas as versões instaladas como atualização.
3. Incrementar o **`versionCode`** em cada nova versão distribuída e atualizar o `versionName` quando apropriado.
4. Preservar o banco Room e utilizar **migrações não destrutivas** se o esquema mudar.
5. Explicar como instalar um novo APK **por cima do antigo, sem desinstalar o aplicativo e sem limpar dados**.
6. Explicar a diferença entre APK `debug` e APK `release`: suas assinaturas podem ser diferentes e, portanto, **não devem ser alternadas como se fossem atualizações compatíveis**.
7. Priorizar um fluxo simples para gerar **APKs release assinados com uma chave pessoal estável** para o uso cotidiano.
8. Explicar como criar e guardar a **keystore** e suas senhas de maneira segura, **fora do Git**, sem embutir credenciais no código-fonte.
9. Informar claramente que perder a chave de assinatura ou mudar o `applicationId` pode impedir a atualização direta; por isso, o backup JSON deve estar disponível.

**Importante:** a atualização que conserva os dados pressupõe que o Android aceite o novo APK como atualização da mesma aplicação. Não prometer preservação dos dados após desinstalação: desinstalar normalmente remove os dados internos do app.

### Instruções de build e instalação exigidas no README

- Pré-requisitos para compilar no **Ubuntu/Linux**, inclusive JDK e Android SDK/Android Studio.
- Como abrir o projeto no Android Studio.
- Como gerar um APK de debug para testes, por exemplo usando `./gradlew assembleDebug`.
- Como gerar um **APK release assinado**, usando sempre a mesma chave.
- Onde localizar cada APK gerado.
- Como instalar no celular manualmente, inclusive a permissão Android para instalar APKs de fontes autorizadas pelo usuário.
- Como instalar ou atualizar via ADB, com um exemplo usando `adb install -r` e o caminho real do arquivo produzido.
- Como aumentar `versionCode` e atualizar o app mantendo os dados.
- Como usar exportação/importação JSON como backup ou recuperação após reinstalação.

---

## 7. Qualidade, testes e critérios de aceite

A implementação só deve ser considerada concluída quando os seguintes cenários funcionarem:

- [ ] Criar três fichas personalizadas (A, B e C), cada uma com vários exercícios.
- [ ] Editar, reordenar e excluir uma ficha sem corromper as demais.
- [ ] Definir séries, repetições e descanso distintos em exercícios diferentes.
- [ ] Buscar exercícios pela API e mostrar GIF/instruções quando disponíveis.
- [ ] Continuar usando fichas, sessões e histórico sem internet.
- [ ] Cadastrar e usar um exercício manualmente.
- [ ] Iniciar uma ficha, marcar/desmarcar exercícios, pausar/continuar descanso e finalizar.
- [ ] Visualizar o tempo de treino corretamente após bloquear a tela ou alternar entre apps.
- [ ] Recuperar uma sessão em andamento após recriação da Activity ou encerramento inesperado do processo.
- [ ] Visualizar no calendário o dia, a ficha usada e os exercícios feitos.
- [ ] Mostrar duas sessões concluídas no mesmo dia.
- [ ] Consultar histórico anterior após renomear ou excluir a ficha original.
- [ ] Exibir duração média quando houver sessões concluídas.
- [ ] Alternar entre temas e entre português e inglês.
- [ ] Exportar JSON válido e importá-lo, restaurando os dados esperados.
- [ ] Atualizar um APK com o mesmo `applicationId` e a mesma assinatura, com `versionCode` novo, **sem perder dados locais**.
- [ ] Compilar o projeto com o Gradle Wrapper sem erros, em ambiente Android corretamente configurado.

Criar testes automatizados para as regras de negócio e persistência mais importantes, em especial cálculo de duração, integridade do histórico, migrações e validação da importação JSON. Se não houver emulador ou SDK configurado no ambiente de execução, **não afirmar que o APK foi compilado ou testado**; fornecer o projeto e os comandos necessários, relatando as verificações efetivamente executadas.

---

## 8. Fora do escopo da primeira versão

Evitar atrasar o MVP com funcionalidades não solicitadas, como:

- Login e contas de usuário.
- Backend próprio, Firebase ou sincronização em nuvem.
- Rede social, compartilhamento de treinos ou anúncios.
- Integração com relógios ou sensores.
- Planos de treinamento gerados por IA.
- Estatísticas avançadas, gráficos de progressão de carga ou acompanhamento de medidas corporais.
- Registro detalhado de peso e repetições reais por série (pode ser implementado no futuro).

A arquitetura pode permitir essas melhorias futuramente, mas **não é necessário implementá-las agora**.

---

## 9. Entregáveis finais para o Codex

Ao concluir, fornecer:

1. **Todo o código-fonte** do projeto Android em uma estrutura organizada.
2. Arquivos Gradle e **Gradle Wrapper**, permitindo abrir e compilar o projeto.
3. `README.md` claro, em **português**, explicando arquitetura, dependências, instalação, uso básico, API, limitações conhecidas, geração e assinatura do APK.
4. Instruções de **backup JSON** e de **atualização sem perda de dados**.
5. Testes implementados e resultado dos testes realmente executados.
6. Se o ambiente permitir, **APK gerado** e seu caminho exato. Caso não permita, explicar objetivamente o impedimento e dar o comando necessário para gerar o APK localmente.

**Ordem de prioridade:** (1) fichas e armazenamento local; (2) execução de sessões e cronômetros; (3) calendário e histórico; (4) importação/exportação; (5) integração e demonstrações ExerciseDB; (6) polimento visual e testes finais. Manter todos os requisitos do MVP no resultado final.

O objetivo é um aplicativo realmente utilizável no meu celular, não apenas uma demonstração visual.
