import { Component, Inject, OnInit, ViewChild, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule, MatDialog } from '@angular/material/dialog';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PortfolioItemResponseDto } from '../../../interfaces/portfolio.dto';
import { ApiService } from '../../../services/api.service';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

@Component({
  selector: 'app-admin-portfolio-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule
  ],
  templateUrl: './admin-portfolio-dialog.component.html',
  styleUrls: ['./admin-portfolio-dialog.component.scss']
})
export class AdminPortfolioDialogComponent implements OnInit {
  @ViewChild('imagePreviewTemplate') imagePreviewTemplate!: TemplateRef<any>;

  form: FormGroup;
  isEditMode = false;
  isSubmitting = false;

  existingImages: string[] = [];
  newFiles: File[] = [];
  newPreviews: string[] = [];
  selectedPreviewImage: string | null = null;

  // Reference to the active preview dialog
  previewRef: MatDialogRef<any> | null = null;

  filteredCategories: Observable<string[]> | undefined;
  allCategories: string[] = [];

  constructor(
    private fb: FormBuilder,
    private apiService: ApiService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<AdminPortfolioDialogComponent>,
    public dialog: MatDialog,
    @Inject(MAT_DIALOG_DATA) public data: { item?: PortfolioItemResponseDto, existingCategories: string[] }
  ) {
    this.allCategories = data.existingCategories || [];

    this.form = this.fb.group({
      category: ['', [Validators.required, Validators.maxLength(50)]],
      description: ['', [Validators.maxLength(500)]]
    });

    if (data.item) {
      this.isEditMode = true;
      this.form.patchValue({
        category: data.item.category,
        description: data.item.description
      });
      this.existingImages = [...data.item.imageUrls];
    }
  }

  ngOnInit() {
    this.filteredCategories = this.form.get('category')!.valueChanges.pipe(
      startWith(''),
      map(value => this._filter(value || ''))
    );
  }

  private _filter(value: string): string[] {
    const filterValue = value.toLowerCase();
    return this.allCategories.filter(option => option.toLowerCase().includes(filterValue));
  }

  onFilesSelected(event: any) {
    const files: FileList = event.target.files;
    if (files) {
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        if (file.type.startsWith('image/')) {
          this.newFiles.push(file);
          const reader = new FileReader();
          reader.onload = (e: any) => this.newPreviews.push(e.target.result);
          reader.readAsDataURL(file);
        }
      }
    }
  }

  removeNewFile(index: number) {
    this.newFiles.splice(index, 1);
    this.newPreviews.splice(index, 1);
  }

  removeExistingImage(index: number) {
    this.existingImages.splice(index, 1);
  }

  // --- Fullscreen Preview Logic ---

  viewImage(url: string) {
    this.selectedPreviewImage = url;
    // Open the preview and store the reference
    this.previewRef = this.dialog.open(this.imagePreviewTemplate, {
      panelClass: 'image-preview-dialog',
      maxWidth: '95vw',
      maxHeight: '95vh',
      backdropClass: 'dark-backdrop',
      autoFocus: false
    });
  }

  closePreview() {
    // Only close the preview dialog
    if (this.previewRef) {
      this.previewRef.close();
      this.previewRef = null;
    }
  }

  // --- Submit Logic ---

  onSubmit() {
    if (this.form.invalid) return;
    if (this.existingImages.length === 0 && this.newFiles.length === 0) {
      this.snackBar.open('You must have at least one image.', 'Close', { duration: 3000 });
      return;
    }

    this.isSubmitting = true;
    const formData = new FormData();

    const dto = {
      ...this.form.value,
      retainedImageUrls: this.existingImages
    };

    formData.append('data', new Blob([JSON.stringify(dto)], { type: 'application/json' }));
    this.newFiles.forEach(file => formData.append('files', file));

    if (this.isEditMode) {
      this.apiService.put(`portfolio/${this.data.item!.id}`, formData).subscribe({
        next: (res) => {
          this.snackBar.open('Updated successfully', 'Close', { duration: 3000 });
          this.dialogRef.close(res);
        },
        error: (err) => {
          console.error(err);
          this.snackBar.open('Failed to update', 'Close', { duration: 3000 });
          this.isSubmitting = false;
        }
      });
    } else {
      this.apiService.post('portfolio', formData).subscribe({
        next: (res) => {
          this.snackBar.open('Created successfully', 'Close', { duration: 3000 });
          this.dialogRef.close(res);
        },
        error: (err) => {
          console.error(err);
          this.snackBar.open('Failed to create', 'Close', { duration: 3000 });
          this.isSubmitting = false;
        }
      });
    }
  }
}
