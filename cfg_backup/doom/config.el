;;; $DOOMDIR/config.el -*- lexical-binding: t; -*-

;; Place your private configuration here! Remember, you do not need to run 'doom
;; sync' after modifying this file!



;; ;; To update version control regularly, even if the file is not modified.
;; (setq auto-revert-check-vc-info t)


;; ;; Configure projectile
;; ;; See `https://docs.projectile.mx/projectile/configuration.html', for details
(after! projectile
  (setq projectile-create-missing-test-files t) ;; Create the test file if does not exist
  (setq projectile-auto-discover nil)  ;; Don't create projectile project automatically.
  (setq projectile-sort-order 'access-time)  ;; Projectile shows files sorted with access time
  (add-to-list 'projectile-globally-ignored-directories "~")  ;; User directory is not considered as a project
  (add-to-list 'projectile-globally-ignored-directories "~/Dev/hephaistox/monorepo") ;; Monorepo is not considered as a project
  (setq projectile-project-root-files-bottom-up (remove ".git"
                                                        projectile-project-root-files-bottom-up))) ;; Our monorepo may have some `.git' subdirectories and there are not projects, just temporary copies.


;; ;; Cider setup
;; ;; See `https://docs.cider.mx/cider/config/basic_config.html' for details
;; ;; (use-package! cider
;; ;;   :after clojure-mode
;; ;;   :config
;; ;;   (set-lookup-handlers! 'cider-mode nil))
(after! cider
  (setq cider-eldoc-display-for-symbol-at-point nil)
  (setq cider-auto-test-mode t) ;; Automatically run tests when compiling a namespace.
  (setq cider-default-cljs-repl 'shadow) ;; Shadow is the default cljs repl.
  (setq cider-comment-prefix "") ;; Remove comment prefix as I use it to generate test values.
  (setq cider-comment-continued-prefix "")  ;; Same for next lines.
  (setq cider-stacktrace-default-filters '(project clj tooling dup java repl)) ;; when an exception is raised default stacktrace filters show only project clojure lines.
  (setq cider-test-fail-fast nil) ;; Fail fast means we don't see all namespaces errors but stop at the first failing deftest. Which is problematic as they are not executed in the order of the namespace.
  (setq cider-repl-pop-to-buffer-on-connect nil) ;; Doesn't show the repl on connection
  (setq cider-clojure-cli-parameters "-J-XX:-OmitStackTraceInFastThrow")) ;; parameters when launching a repl.  -J-XX:-OmitStackTraceInFastThrow means all exceptions will be raised, default jvm options is to stop outputiing them.


;; ;; cljr is used together with lsp.
;; ;; See `https://github.com/clojure-emacs/clj-refactor.el/wiki#customization' for details
(after! clj-refactor
  ;; Settings to automatically insert headers of test files.
  (setq cljr-expectations-test-declaration
        "[clojure.test :refer [deftest is testing]]")
  (setq cljr-clojure-test-declaration
        "[clojure.test :refer [deftest is testing]]")
  (setq cljr-cljs-clojure-test-declaration
        "[cljs.test :refer [deftest is testing] :include-macros true]")
  (setq cljr-cljc-clojure-test-declaration
        "#?(:clj [clojure.test :refer [deftest is testing]]
 :cljs [cljs.test :refer [deftest is testing] :include-macros true])"))
;; (use-package! clj-refactor
;;   :after clojure-mode
;;   :config
;;   (set-lookup-handlers! 'clj-refactor-mode nil))

;; (use-package tree-sitter
;;   :config
;;   (add-to-list 'tree-sitter-major-mode-language-alist '(clojurec-mode . clojure))
;;   (add-to-list 'tree-sitter-major-mode-language-alist '(clojurescript-mode . clojure)))

(setq auth-sources '("~/.authinfo.gpg"))

;; Mermaid-mode
;; Automatically activate mermaid-mode as a major mode when opening `.mermaid' suffix files.
(add-to-list 'auto-mode-alist '("\\.mermaid\\'" . mermaid-mode))


;; lsp grammarly
(use-package! lsp-grammarly
  :ensure t
  :hook (text-mode . (lambda ()
                       (require 'lsp-grammarly)
                       (lsp)
                       (set-lsp-priority! 'lsp-grammarly 2))))

;; lsp clojure
(after! lsp-clojure
  (setq lsp-lens-enable nil))

;; Some functionality uses this to identify you, e.g. GPG configuration, email
;; clients, file templates and snippets. It is optional.
(setq user-full-name "Anthony CAUMOND"
      user-mail-address "anthony@caumond.com")

;; Doom exposes five (optional) variables for controlling fonts in Doom:
;;
;; - `doom-font' -- the primary font to use
;; - `doom-variable-pitch-font' -- a non-monospace font (where applicable)
;; - `doom-big-font' -- used for `doom-big-font-mode'; use this for
;;   presentations or streaming.
;; - `doom-symbol-font' -- for symbols
;; - `doom-serif-font' -- for the `fixed-pitch-serif' face
;;
;; See 'C-h v doom-font' for documentation and more examples of what they
;; accept. For example:
;;
;;(setq doom-font (font-spec :family "Fira Code" :size 12 :weight 'semi-light)
;;      doom-variable-pitch-font (font-spec :family "Fira Sans" :size 13))
;;
;; If you or Emacs can't find your font, use 'M-x describe-font' to look them
;; up, `M-x eval-region' to execute elisp code, and 'M-x doom/reload-font' to
;; refresh your font settings. If Emacs still can't find your font, it likely
;; wasn't installed correctly. Font issues are rarely Doom issues!

;; There are two ways to load a theme. Both assume the theme is installed and
;; available. You can either set `doom-theme' or manually load a theme with the
;; `load-theme' function. This is the default:
(setq doom-theme 'doom-dracula)

;; This determines the style of line numbers in effect. If set to `nil', line
;; numbers are disabled. For relative line numbers, set this to `relative'.
(setq display-line-numbers-type t)

;; If you use `org' and don't want your org files in the default location below,
;; change `org-directory'. It must be set before org loads!
;; Org non active anymore so commented out
;; (setq org-directory "~/Dev/perso/my-notes/todos")
;; (setq! org-agenda-files
;;        '("~/Dev/perso/my-notes/todos/main.org"))
(setq org-directory "~/org/")

(after! org
  (setq org-duration-format (quote h:mm)))
;; Whenever you reconfigure a package, make sure to wrap your config in an
;; `after!' block, otherwise Doom's defaults may override your settings. E.g.
;;
;;   (after! PACKAGE
;;     (setq x y))
;;
;; The exceptions to this rule:
;;
;;   - Setting file/directory variables (like `org-directory')
;;   - Setting variables which explicitly tell you to set them before their
;;     package is loaded (see 'C-h v VARIABLE' to look up their documentation).
;;   - Setting doom variables (which start with 'doom-' or '+').
;;
;; Here are some additional functions/macros that will help you configure Doom.
;;
;; - `load!' for loading external *.el files relative to this one
;; - `use-package!' for configuring packages
;; - `after!' for running code after a package has loaded
;; - `add-load-path!' for adding directories to the `load-path', relative to
;;   this file. Emacs searches the `load-path' when you load packages with
;;   `require' or `use-package'.
;; - `map!' for binding new keys
;;
;; To get information about any of these functions/macros, move the cursor over
;; the highlighted symbol at press 'K' (non-evil users must press 'C-c c k').
;; This will open documentation for it, including demos of how they are used.
;; Alternatively, use `C-h o' to look up a symbol (functions, variables, faces,
;; etc).
;;
;; You can also try 'gd' (or 'C-c c d') to jump to their definition and see how
;; they are implemented.
(setq! initial-frame-alist
       '((fullscreen . maximized)))

;; C Control
;; M Option
;; s Command
;; S Shift
(map! "C-M-;" #'babashka-project-tasks)
(map! "M-s-t" #'projectile-toggle-between-implementation-and-test)
(map! "C-M-v" #'sp-forward-slurp-sexp)
(map! "C-k" #'sp-kill-sexp)
(map! "C-M-k" #'sp-kill-word)
(map! "C-M-s-r" #'sp-raise-sexp)
(map! "C-M-è" #'sp-wrap-curly)
(map! "C-M-(" #'sp-wrap-round)
(map! "C-M-§" #'sp-wrap-square)

(map! :nvi "s-r" #'evil-multiedit-match-all
      :nvi "s-d" #'evil-multiedit-match-symbol-and-next
      :nvi "s-D" #'evil-multiedit-match-symbol-and-prev)
