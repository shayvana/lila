import * as xhr from 'common/xhr';
import throttle from 'common/throttle';
import spinner from './component/spinner';
import Editor from '@toast-ui/editor';

lichess.load.then(() => {
  $('.flash').addClass('fade');
  $('.ublog-post-form__image').each(function (this: HTMLFormElement) {
    const form = this;
    $(form)
      .find('input[name="image"]')
      .on('change', () => {
        const replace = (html: string) => $(form).find('.ublog-post-image').replaceWith(html);
        const wrap = (html: string) => '<div class="ublog-post-image">' + html + '</div>';
        replace(wrap(spinner));
        xhr.formToXhr(form).then(
          html => replace(html),
          err => replace(wrap(`<bad>${err}</bad>`))
        );
      });
  });
  $('#markdown-editor').each(function (this: HTMLTextAreaElement) {
    const el = this,
      editor: Editor = new Editor({
        el,
        usageStatistics: false,
        height: '70vh',
        theme: $('body').data('theme') == 'light' ? 'light' : 'dark',
        initialValue: $('#form3-markdown').val() as string,
        initialEditType: 'wysiwyg',
        language: $('html').attr('lang') as string,
        toolbarItems: [
          ['heading', 'bold', 'italic', 'strike'],
          ['hr', 'quote'],
          ['ul', 'ol'],
          ['table', 'image', 'link'],
          ['code', 'codeblock'],
          ['scrollSync'],
        ],
        autofocus: false,
        events: {
          change: throttle(500, () => $('#form3-markdown').val(postProcess(editor.getMarkdown()))),
        },
        hooks: {
          addImageBlobHook() {
            alert('Sorry, file upload in the post body is not supported. Only image URLs will work.');
          },
        },
      });
    // in a modal, <Enter> should complete the action, not submit the post form
    $(el).on('keypress', event => {
      if (event.key != 'Enter') return;
      const okButton = $(event.target).parents('.toastui-editor-popup-body').find('.toastui-editor-ok-button')[0];
      if (okButton) $(okButton).trigger('click');
      return !okButton;
    });
    $(el)
      .find('button.image')
      .on('click', () => {
        $(el).find('.toastui-editor-popup-add-image .tab-item:last-child').trigger('click');
        $('#toastuiImageUrlInput')[0]?.focus();
      });
    $(el)
      .find('button.link')
      .on('click', () => $('#toastuiLinkUrlInput')[0]?.focus());
  });
});

const postProcess = (markdown: string) => markdown.replace(/<br>/g, '').replace(/\n\s*#\s/g, '\n## ');